import json
import re
import sys
import shutil
from pathlib import Path


def strip_all_tags(text):
    """Remove HTML tags from text."""
    return re.sub(r'<[^>]*>', '', text)

def replace_parens(match):
    before = match.group(1) or ''
    content = match.group(2)
    after = match.group(3) or ''

    lead = '' if before == '_' else '_'
    trail = '' if after == '_' else '_'

    return f"{before}{lead}{content}{trail}{after}"

def main(raw_file, tests_dir, tests_aux_dir, datasets_dir):
    # Read input JSON file
    with open(raw_file, 'r', encoding='utf-8') as f:
        datas = json.load(f)

    # Clean target directories
    datasets_path = tests_aux_dir / datasets_dir
    if datasets_path.exists():
        shutil.rmtree(datasets_path)
    datasets_path.mkdir()

    if tests_dir.exists():
        shutil.rmtree(tests_dir)
    tests_dir.mkdir()

    k = 0
    for data in datas.values():
        dataset = []
        dataset_name = f"{data['paper_id']}-{k}"
        k += 1

        keys = data['table_column_names']
        values = data['table_content_values']

        # Build dataset rows
        for value in values:
            row = {}
            unnamed_counter = 1
            for i in range(len(value)):
                # Remove HTML tags from keys and values
                clean_key = strip_all_tags(keys[i])
                clean_value = strip_all_tags(value[i])

                clean_value = clean_value.strip()
                clean_key = clean_key.strip()

                # Clean key: replace spaces with underscores, remove [xxx] tags,
                # ensure first character is lowercase
                tmp_key = re.sub(r'\[\w+\]', 'key', clean_key.replace(' ', '_'))
                # cleaned_key = tmp_key[:1].lower() + tmp_key[1:]
                cleaned_key = tmp_key

                if not tmp_key[:1].islower():
                    cleaned_key = "_" + cleaned_key

                # Replace parentheses by underscores (omitting underscore if already present)
                cleaned_key = re.sub(r'(_)?\(([^)]*)\)(_)?', replace_parens, cleaned_key)

                # Replace # with 'num_'
                cleaned_key = re.sub(r'#', 'num_', cleaned_key)

                # Replace special characters with underscore
                cleaned_key = re.sub(r'[/\.\-\*→%,\:\;\\\'"+<>@&\|\!\?\$\^`~]', '_', cleaned_key)

                # Remove trailing underscores and collapse multiple underscores
                cleaned_key = re.sub(r'_+', '_', cleaned_key).rstrip('_')

                # If key is empty, assign a default name with counter
                if not cleaned_key or cleaned_key.strip() == '':
                    cleaned_key = f'_{unnamed_counter}'
                    unnamed_counter += 1

                # Remove bracket tags of the form [word]
                cleaned_value = re.sub(r'\[\w+\]', '', clean_value)

                # Replace unicode U+2004 (THREE-PER-EM SPACE) with regular space for consistent processing
                cleaned_value = cleaned_value.replace('\u2004', ' ')

                # Remove ∼ prefix only when before a number (not between numbers)
                cleaned_value = re.sub(r'(^|\s)∼\s*(?=\d)', r'\1', cleaned_value)

                # Remove commas used as thousands separators (e.g., "18,000" -> "18000", "7,123K" -> "7123K")
                cleaned_value = re.sub(r',(?=\d)', '', cleaned_value)

                # Remove asterisks adjacent to numbers or after spaces (e.g., "**52.4**" -> "52.4", "2.19e-14 ***" -> "2.19e-14")
                cleaned_value = re.sub(r'[\*⋆∗]+(?=\d)|(?<=\d)[\*⋆∗]+', '', cleaned_value)
                cleaned_value = re.sub(r'\s+[\*⋆∗]+\s*$', '', cleaned_value)  # Remove trailing asterisks after spaces

                # Remove dagger-like symbols (†, ‡, ⋄) immediately following a digit
                cleaned_value = re.sub(r'(?<=\d)[†‡⋄]+', '', cleaned_value)

                # Reduce "n op anything" to "n" for op in {±, ↓}
                cleaned_value = re.sub(r'^\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*[±↓↑⇑]\s*.*$', r'\1', cleaned_value)

                # Reduce "n / m / k", "n | m | k", or "n, m, k" to "n"
                # Exclude some files from this rule
                print(dataset_name)
                if dataset_name not in ["1707.03103v2-267", "1906.11565v2-387", "1908.11049v1-345"]:
                    cleaned_value = re.sub(
                        r'^\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)(?:\s*[/|,]\s*-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)*\s*$',
                        r'\1',
                        cleaned_value
                    )

                # Remove parenthetical information after numbers, optionally followed by •
                cleaned_value = re.sub(r'^(\s*-?\d+(?:\.\d+)?%?)\s*\([^)]+\)\s*•?\s*$', r'\1', cleaned_value)

                # Discard any suffix starting with \scalebox
                cleaned_value = re.sub(r'\s*\\scalebox.*$', '', cleaned_value)

                # If the value starts with a number, discard any trailing garbage that looks like
                # a space followed by digits and/or decimal points (e.g. "96.9 97.296.5" → "96.9")
                match = re.match(
                    r'^\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)(?:\s+[\d\.]+)?\s*$',
                    cleaned_value
                )
                if match:
                    cleaned_value = match.group(1)

                # Remove trailing (↑, ⇑) when they follow a number
                cleaned_value = re.sub(r'(?<=\d)\s*[↑⇑]\s*$', '', cleaned_value)

                # Remove K/k/x/× suffix only when the entire value is numeric
                cleaned_value = re.sub(r'^\s*(-?\d+(?:\.\d+)?)[KkxX×]\s*$', r'\1', cleaned_value)

                # Remove terminal dash sequence only if it follows a digit or %
                cleaned_value = re.sub(r'^(.+[0-9%])\s*[–—−]+$', r'\1', cleaned_value)

                # Remove terminal %
                cleaned_value = re.sub(r'%$', '', cleaned_value)

                # Remove common units of measurement only when preceded by digits
                cleaned_value = re.sub(r'(\d)\s*(TB|GB|MB|KB|PB|GHz|MHz|KHz|Hz|ms|μs|ns)$', r'\1', cleaned_value, flags=re.IGNORECASE)

                # Convert values like "2.51M" -> 2510000
                m = re.match(r'^\s*(-?\d+(?:\.\d+)?)M\s*$', cleaned_value)
                if m:
                    cleaned_value = str(float(m.group(1)) * 1_000_000)

                # Convert to numeric type if it's a valid number
                try:
                    # Try to convert to float first (handles scientific notation like 5.48e-15)
                    if '.' in cleaned_value or 'e' in cleaned_value.lower():
                        cleaned_value = float(cleaned_value)
                    else:
                        # Try integer
                        cleaned_value = int(cleaned_value)
                except (ValueError, AttributeError):
                    # Keep as string if conversion fails
                    pass

                row[cleaned_key] = cleaned_value
            dataset.append(row)

        # Create test structure
        test = {
            'datasets': [f"{datasets_dir.as_posix()}/{dataset_name}.json"],
            'imports': [
                "scigen",
                "util",
                f"{datasets_dir.as_posix()}/_{dataset_name.replace('-', '_').replace('.', '_')}"
            ],
            'variables': {},
            'testing-variables': {}
        }

        # Clean text by removing [xxx] tags
        text = re.sub(r'\[\w+\]', '', data['text'])

        # Create paragraph with only literal (no number search)
        paragraph = {
            "type": "literal",
            "value": text
        }
        test['paragraph'] = [paragraph]

        # Write dataset JSON file
        dataset_file = datasets_path / f"{dataset_name}.json"
        with open(dataset_file, 'w', encoding='utf-8') as f:
            json.dump(dataset, f, indent=2, ensure_ascii=False)
            f.write('\n')
            
        # Write .fld file with loadJson instruction
        fld_name = f"_{dataset_name.replace('-', '_').replace('.', '_')}"
        fld_content = f'let tableData = loadJson "{datasets_dir.as_posix()}/{dataset_name}.json";'
        fld_file = datasets_path / f"{fld_name}.fld"
        with open(fld_file, 'w', encoding='utf-8') as f:
            f.write(fld_content)

        # Write test JSON and empty .fld file into tests_dir
        test_file = tests_dir / f"{dataset_name}.json"
        with open(test_file, 'w', encoding='utf-8') as f:
            json.dump(test, f, indent=2, ensure_ascii=False)

        test_fld_file = tests_dir / f"{dataset_name}.fld"
        with open(test_fld_file, 'w', encoding='utf-8') as f:
            f.write("")

        print(f"Generated: {dataset_name}")


if __name__ == "__main__":
    # Run from root of repo
    main(
      raw_file=Path("script/scigen-gen/raw/scigen.json"),
      tests_dir=Path("testCases/scigen-raw"),
      tests_aux_dir=Path("testCases-aux"),
      datasets_dir=Path("datasets/scigen")
    )
