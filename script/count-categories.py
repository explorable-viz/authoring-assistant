#!/usr/bin/env python3


import json
import os
import sys
from collections import defaultdict
from pathlib import Path


def extract_categories_from_file(json_file):
    categories = []    
    try:
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        if 'paragraph' in data:
            for item in data['paragraph']:
                if item.get('type') == 'expression' and 'categories' in item:
                    # Categories can be a list
                    cats = item['categories']
                    if isinstance(cats, list):
                        categories.extend(cats)
                    else:
                        categories.append(cats)
    
    except Exception as e:
        print(f"Warning: Error reading {json_file}: {e}", file=sys.stderr)
    
    return categories


def count_expressions_per_category(test_cases_folder):
    category_expressions = defaultdict(set)
    expression_category_counts = {}
    
    total_expressions = 0
    total_files = 0
    
    test_cases_path = Path(test_cases_folder)
    
    if not test_cases_path.exists():
        print(f"Error: Folder '{test_cases_folder}' does not exist", file=sys.stderr)
        return
    
    json_files = list(test_cases_path.rglob('*.json'))
    
    if not json_files:
        print(f"No JSON files found in '{test_cases_folder}'", file=sys.stderr)
        return
    
    for json_file in json_files:
        total_files += 1
        categories = extract_categories_from_file(json_file)
        
        if categories:
            expr_id = str(json_file.relative_to(test_cases_path))
            unique_categories = list(set(categories))
            expression_category_counts[expr_id] = len(unique_categories)
            
            for category in categories:
                category_expressions[category].add(expr_id)
            
            total_expressions += 1
    
    # Display results
    print(f"\n{'='*60}")
    print(f"Test Cases Folder: {test_cases_folder}")
    print(f"{'='*60}")
    print(f"Total JSON files processed: {total_files}")
    print(f"Total files with expressions: {total_expressions}")
    print(f"\n{'='*60}")
    print("Distribution of Expressions per Category:")
    print(f"{'='*60}\n")
    
    # Sort by count (descending) then by name
    sorted_categories = sorted(
        category_expressions.items(),
        key=lambda x: (-len(x[1]), x[0])
    )
    
    for category, expressions in sorted_categories:
        count = len(expressions)
        percentage = (count / total_expressions * 100) if total_expressions > 0 else 0
        print(f"  {category:25s}: {count:4d} expressions ({percentage:5.1f}%)")
    
    print(f"\n{'='*60}")
    print(f"Total unique categories: {len(category_expressions)}")
    print(f"{'='*60}\n")
    
    print(f"{'='*60}")
    print("Expressions by Number of Categories:")
    print(f"{'='*60}\n")
    
    category_count_distribution = defaultdict(int)
    for expr_id, num_categories in expression_category_counts.items():
        category_count_distribution[num_categories] += 1
    
    sorted_distribution = sorted(category_count_distribution.items())
    
    for num_cats, num_exprs in sorted_distribution:
        percentage = (num_exprs / total_expressions * 100) if total_expressions > 0 else 0
        print(f"  {num_cats} {'category' if num_cats == 1 else 'categories':12s}: {num_exprs:4d} expressions ({percentage:5.1f}%)")
    
    multi_category = sum(count for num_cats, count in category_count_distribution.items() if num_cats > 1)
    multi_percentage = (multi_category / total_expressions * 100) if total_expressions > 0 else 0
    
    print(f"\n  Total with multiple categories: {multi_category:4d} expressions ({multi_percentage:5.1f}%)")
    print(f"\n{'='*60}\n")
    
    print(f"\n{'='*60}")
    print(f"Total unique categories: {len(category_expressions)}")
    print(f"{'='*60}\n")


def main():
    if len(sys.argv) < 2:
        print("Usage: python count-categories.py <test-cases-folder>")
        print("\nExample:")
        print("  python count-categories.py ./testCases/scigen-manual")
        sys.exit(1)
    
    test_cases_folder = sys.argv[1]
    count_expressions_per_category(test_cases_folder)


if __name__ == "__main__":
    main()
