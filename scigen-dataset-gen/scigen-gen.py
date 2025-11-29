import json
import re
import sys
from pathlib import Path


def strip_all_tags(text):
    """Remove HTML tags from text."""
    return re.sub(r'<[^>]*>', '', text)


def main(raw_file, tests_dir, datasets_dir):
    # Read input JSON file
    with open(raw_file, 'r', encoding='utf-8') as f:
        datas = json.load(f)
    
    # Create output directories
    datasets_dir.mkdir(parents=True, exist_ok=True)
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
            for i in range(len(value)):
                # Remove HTML tags from keys and values
                clean_key = strip_all_tags(keys[i])
                clean_value = strip_all_tags(value[i])
                
                # Clean key: lowercase, replace spaces with underscores, remove [xxx] tags
                cleaned_key = re.sub(r'\[\w+\]', 'key', clean_key.replace(' ', '_').lower())
                cleaned_value = re.sub(r'\[\w+\]', '', clean_value)
                
                row[cleaned_key] = cleaned_value
            dataset.append(row)
        
        # Create test structure
        test = {
            'datasets': [f"{datasets_dir}/{dataset_name}.json"],
            'imports': [
                "scigen",
                "util",
                f"{datasets_dir}/_{dataset_name.replace('-', '_').replace('.', '_')}"
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
        dataset_file = datasets_dir / f"{dataset_name}.json"
        with open(dataset_file, 'w', encoding='utf-8') as f:
            json.dump(dataset, f, indent=2, ensure_ascii=False)
        
        # Write .fld file with loadJson instruction
        fld_name = f"_{dataset_name.replace('-', '_').replace('.', '_')}"
        fld_content = f'let tableData = loadJson "{datasets_dir}/{dataset_name}.json";'
        fld_file = datasets_dir / f"{fld_name}.fld"
        with open(fld_file, 'w', encoding='utf-8') as f:
            f.write(fld_content)
        
        # Write test JSON file
        test_file = tests_dir / f"{dataset_name}.json"
        with open(test_file, 'w', encoding='utf-8') as f:
            json.dump(test, f, indent=2, ensure_ascii=False)
        
        # Write empty .fld file in tests/
        test_fld_file = tests_dir / f"{dataset_name}.fld"
        with open(test_fld_file, 'w', encoding='utf-8') as f:
            f.write("")
        
        print(f"Generated: {dataset_name}")


if __name__ == "__main__":
    # Parse command-line arguments
    if len(sys.argv) != 4:
        print("Usage: python scigen-gen.py <raw_dataset_file> <test_output_folder> <dataset_output_folder>")
        sys.exit(1)
    
    raw_file = Path(sys.argv[1])
    tests_dir = Path(sys.argv[2])
    datasets_dir = Path(sys.argv[3])
    
    main(raw_file, tests_dir, datasets_dir)
