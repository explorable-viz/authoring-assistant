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

                # Clean key: lowercase, replace spaces with underscores, remove [xxx] tags
                cleaned_key = re.sub(r'\[\w+\]', 'key', clean_key.replace(' ', '_').lower())

                # Replace parentheses by underscores (omitting underscore if already present)
                cleaned_key = re.sub(r'(_)?\(([^)]*)\)(_)?', replace_parens, cleaned_key)

                # Replace # with 'num' when it represents "number of" in keys
                cleaned_key = re.sub(r'#', 'num_', cleaned_key)

                # Replace special characters with underscore
                cleaned_key = re.sub(r'[/\.\-\*→%,\:\;\\\'"+<>@&\|\!\?\$\^`~]', '_', cleaned_key)

                # Remove leading/trailing underscores and collapse multiple underscores
                cleaned_key = re.sub(r'_+', '_', cleaned_key).strip('_')

                # If key is empty, assign a default name with counter
                if not cleaned_key or cleaned_key.strip() == '':
                    cleaned_key = f'_{unnamed_counter}'
                    unnamed_counter += 1

                cleaned_value = re.sub(r'\[\w+\]', '', clean_value)

                # Replace unicode U+2004 (THREE-PER-EM SPACE) with regular space for consistent processing
                cleaned_value = cleaned_value.replace('\u2004', ' ')

                # Remove parenthesised terms only when they follow a number and contain specific units like (s) or (%)
                # cleaned_value = re.sub(r'(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*\([s%]\)', r'\1', cleaned_value)

                # Remove ∼ prefix only when before a number (not between numbers)
                cleaned_value = re.sub(r'(^|\s)∼\s*(?=\d)', r'\1', cleaned_value)

                # Extract only the first number when ± symbol is present (e.g., "5.2 ± 0.3" -> "5.2")
                cleaned_value = re.sub(r'(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*±\s*\d+(?:\.\d+)?(?:[eE][+-]?\d+)?', r'\1', cleaned_value)

                # Remove asterisk and star symbols only adjacent to numbers (e.g., "**52.4**" -> "52.4", "96.9⋆" -> "96.9")
                cleaned_value = re.sub(r'[\*⋆]+(?=\d)|(?<=\d)[\*⋆]+', '', cleaned_value)
                
                # Extract only the first number from the string (e.g., "96.9 97.296.5" -> "96.9", " 65.2 68.559.1" -> "65.2")
                match = re.match(r'^\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)', cleaned_value)
                if match:
                    cleaned_value = match.group(1)

                # Remove K/k/x suffix and % symbol only at the end of numeric values
                # Also remove commas from numbers (e.g., 7,123K -> 7123, 1.9x -> 1.9)
                cleaned_value = re.sub(r'(\d)[Kkx]$', r'\1', cleaned_value)  # Remove K, k, or x at the end only after a digit
                cleaned_value = re.sub(r'%$', '', cleaned_value)  # Remove % at the end
                cleaned_value = re.sub(r',(?=\d)', '', cleaned_value)  # Remove commas followed by digits
                
                # Extract only the first number from the string (e.g., "63.7 65.661.6" -> "63.7", "96.9 97.296.5" -> "96.9")
                match = re.match(r'^\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)', cleaned_value)
                if match:
                    cleaned_value = match.group(1)
                cleaned_value = re.sub(r'%$', '', cleaned_value)  # Remove % at the end
                cleaned_value = re.sub(r',(?=\d)', '', cleaned_value)  # Remove commas followed by digits

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
            'datasets': [f"datasets/{dataset_name}.json"],
            'imports': [
                "scigen",
                "util",
                f"datasets/_{dataset_name.replace('-', '_').replace('.', '_')}"
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

        # Write .fld file with loadJson instruction
        fld_name = f"_{dataset_name.replace('-', '_').replace('.', '_')}"
        fld_content = f'let tableData = loadJson "{datasets_dir}/{dataset_name}.json";'
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
