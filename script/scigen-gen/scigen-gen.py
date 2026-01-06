import json
import re
import sys
from pathlib import Path


def strip_all_tags(text):
    """Remove HTML tags from text."""
    return re.sub(r'<[^>]*>', '', text)


def main(raw_file, tests_dir, tests_aux_dir, datasets_dir):
    # Read input JSON file
    with open(raw_file, 'r', encoding='utf-8') as f:
        datas = json.load(f)

    # Create output directories
    datasets_path = tests_aux_dir / datasets_dir
    datasets_path.mkdir(parents=True, exist_ok=True)
    tests_dir.mkdir(parents=True, exist_ok=True)

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

                # Remove parenthesised terms from key anywhere (e.g., (s), (%), etc.)
                cleaned_key = re.sub(r'_?\([^)]*\)_?', '_', cleaned_key)

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

                # Remove parenthesised terms only when they follow a number and contain specific units like (s) or (%)
                cleaned_value = re.sub(r'(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*\([s%]\)', r'\1', cleaned_value)

                # Remove ∼ prefix only when before a number (not between numbers)
                cleaned_value = re.sub(r'(^|\s)∼\s*(?=\d)', r'\1', cleaned_value)

                # Extract only the first number when ± symbol is present (e.g., "5.2 ± 0.3" -> "5.2")
                cleaned_value = re.sub(r'(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*±\s*\d+(?:\.\d+)?(?:[eE][+-]?\d+)?', r'\1', cleaned_value)

                # Remove asterisk before and after numbers including scientific notation (e.g., "**52.4**" -> "52.4", "*5.48e-15" -> "5.48e-15")
                cleaned_value = re.sub(r'\*+(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)', r'\1', cleaned_value)  # Remove leading asterisks
                cleaned_value = re.sub(r'(\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*\*+', r'\1', cleaned_value)  # Remove trailing asterisks

                # Remove K/k suffix and % symbol only at the end of numeric values
                # Also remove commas from numbers (e.g., 7,123K -> 7123)
                cleaned_value = re.sub(r'[Kk]$', '', cleaned_value)  # Remove K or k at the end
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
