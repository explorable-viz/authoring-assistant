import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import glob
import os
import json

def count_problems_per_category(df):
    # Explode expression-type categories (format: "[Category1,Category2]")
    df_exploded = df.copy()
    # Convert to string and handle NaN values
    df_exploded['expression-type'] = df_exploded['expression-type'].astype(str)
    df_exploded['expression-type'] = df_exploded['expression-type'].str.strip('[]').str.split(',')
    df_exploded = df_exploded.explode('expression-type')
    df_exploded['expression-type'] = df_exploded['expression-type'].str.strip()
    
    # Count unique (expected-expression, target-value) pairs per category
    # This counts the actual expressions, not the test case files
    category_counts = df_exploded.groupby("expression-type").apply(
        lambda x: x[['expected-expression', 'target-value']].drop_duplicates().shape[0]
    )
    
    return category_counts

def generate_success_rate_test_case_plot(df, plot, fig_dir):
    summary = (
        df.groupby("test-case-short")
        .agg(success_rate=("success", "mean"), avg_attempts=("attempts", "mean"), num_queries=("test-case-short", "count"))
        .reset_index()
    )
    summary["y_label"] = summary["test-case-short"] + " (" + summary["num_queries"].astype(str) + " queries)"
    plt.figure(figsize=(10, 6))
    ax = sns.barplot(data=summary, x="success_rate", y="y_label")
    plt.title("Success Rate per Test Case (Max attempts = 4)")
    plt.xlabel("Success Rate")
    plt.ylabel("Test Case")
    plt.xlim(0, 1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_test_case.png")
    plt.close()

def generate_aggregated_plot(df, plot, fig_dir):
    # esplodo le categorie
    df['expression-type'] = df['expression-type'].astype(str).str.strip('[]').str.split(',')
    df_exploded = df.explode('expression-type')
    df_exploded['expression-type'] = df_exploded['expression-type'].str.strip()

    # calcolo success_rate SEPARATAMENTE per target-value
    summary = (
        df_exploded.groupby(["expression-type", "target-value"])
        .agg(success_rate=("success", "mean"),
             count=("success", "size"))
        .reset_index()
    )

    # etichette sull’asse x basate sul nome categoria
    plt.figure(figsize=(6,6))
    label_map = {
        1: "Present",
        0: "Absent"
    }
    summary["target_label"] = summary["target-value"].map(label_map)

    ax = sns.barplot(
        data=summary,
        x="expression-type",   # oppure "label"
        y="success_rate",
        hue="target_label",
        palette="Set2"
    )
    ax.legend(title="Target value")

    # annotazioni sopra ogni barra
    for p in ax.patches:
        h = p.get_height()
        ax.annotate(f"{h:.2f}",
                    (p.get_x() + p.get_width()/2., h),
                    ha='center', va='bottom', fontsize=8)

    plt.title("Success Rate by Linguistic Category")
    plt.xlabel("Category")
    plt.ylabel("Average Success Rate")
    plt.xticks(rotation=45, ha="right")
    plt.ylim(0,1.1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_category.png")
    plt.close()

def generate_aggregated_boxplot(df, plot, fig_dir):
    # Use the reusable function to count problems per category BEFORE exploding df
    category_counts = count_problems_per_category(df)
    
    df['expression-type'] = df['expression-type'].astype(str).str.strip('[]').str.split(',')
    df_exploded = df.explode('expression-type')
    df_exploded['expression-type'] = df_exploded['expression-type'].str.strip()

    summary = (
        df_exploded.groupby(["runId", "expression-type", "target-value"])
        .agg(success_rate=("success", "mean"))
        .reset_index()
    )
    
    # labels for  target-value
    plt.figure(figsize=(8,6))
    label_map = {
        1: "Present",
        0: "Absent"
    }
    summary["target_label"] = summary["target-value"].map(label_map)

    ax = sns.boxplot(
        data=summary,
        x="expression-type",
        y="success_rate",
        hue="target_label",
        palette="Set2",
        order=sorted(summary['expression-type'].unique())
    )
    ax.legend(title="Target value", loc='lower right', bbox_to_anchor=(1.0, -0.25))

    # Use categories from summary for iteration
    categories = sorted(summary['expression-type'].unique())
    target_values = sorted(summary['target-value'].unique())
    
    for i, category in enumerate(categories):
        # Get problem count for this category from category_counts
        # category_counts already contains the total unique problems (not per run)
        if category in category_counts.index:
            count = int(category_counts[category])
        else:
            count = 0
        
        for j, target_val in enumerate(target_values):
            x_offset = -0.2 if j == 0 else 0.2
            mask = (summary['expression-type'] == category) & (summary['target-value'] == target_val)
            if mask.sum() > 0:
                median_y = summary[mask]['success_rate'].median()
            else:
                median_y = 0.5  
            
            # Always add the label
            ax.text(i + x_offset, median_y, f'n={count}', 
                    ha='center', va='center', 
                    fontsize=8, color='black', weight='bold',
                    bbox=dict(boxstyle='round,pad=0.2', facecolor='white', alpha=0.8))

    # plt.title("Success Rate by Linguistic Category")
    plt.xlabel("Category")
    plt.ylabel("Success Rate")
    plt.xticks(rotation=45, ha="right")
    plt.ylim(-0.05,1.1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_category_boxplot.png")
    plt.close()


def generate_success_rate_by_category_count(df, plot, fig_dir):
    df['category_count'] = df['expression-type'].str.len()

    summary = (
        df.groupby(["category_count", "target-value"])
        .agg(
            success_rate=("success", "mean"),
            count=("success", "size")
        )
        .reset_index()
    )

    summary["label"] = summary["category_count"].astype(str)

    plt.figure(figsize=(6, 6))
    label_map = {
        1: "Present",
        0: "Absent"
    }
    summary["target_label"] = summary["target-value"].map(label_map)

    ax = sns.barplot(
        data=summary,
        x="label",
        y="success_rate",
        hue="target_label",
        palette="Set2"
    )
    ax.legend(title="Target value")
    for p in ax.patches:
        ax.annotate(f"{p.get_height():.2f}",
                    (p.get_x() + p.get_width() / 2., p.get_height()),
                    ha='center', va='bottom', fontsize=8)

    #plt.title("Success Rate by Complexity")
    plt.xlabel("Complexity")
    plt.ylabel(f"Average Success Rate over {df['runId'].nunique()} runs")
    plt.xticks(rotation=45, ha="right")
    plt.ylim(0, 1.1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_complexity.png")
    plt.close()

def process_csv_file(csv_file):
    """Process a single CSV file and generate charts in the corresponding subdirectory."""
    # Get the subdirectory name (e.g., "scigen-manual" from "results/scigen-manual/file.csv")
    rel_path = os.path.relpath(csv_file, "results")
    subdir = os.path.dirname(rel_path)
    
    # Create output directory
    fig_dir = os.path.join("paper", "fig", subdir)
    os.makedirs(fig_dir, exist_ok=True)
    
    print(f"\n{'='*60}")
    print(f"Processing: {csv_file}")
    print(f"Output directory: {fig_dir}")
    print(f"{'='*60}")
    
    df = pd.read_csv(csv_file, delimiter=';', quotechar='"', encoding='utf-8')
    # Filter out rows where is-negative=true
    df = df[df["is-negative"].astype(str).str.lower() != "true"]
    df["success"] = df["generated-expression"].notna().astype(int)
    df["target-value"] = df["target-value"].astype(int)
    df["attempts"] = pd.to_numeric(df["attempts"], errors="coerce")
    df["duration(ms)"] = pd.to_numeric(df["duration(ms)"], errors="coerce")
    df["test-case-short"] = df["test-case"].apply(
        lambda x: os.path.join(os.path.basename(os.path.dirname(str(x))), os.path.basename(str(x)))
    )
    
    # Print the number of problems per linguistic category
    category_counts = count_problems_per_category(df)
    print("\nNumber of problems per linguistic category:")
    for category, count in category_counts.items():
        print(f"  {category}: {count}")
    print()
    
    sns.set_style("whitegrid")
    generate_success_rate_test_case_plot(df, plt, fig_dir)
    #generate_aggregated_plot(df, plt, fig_dir)
    generate_aggregated_boxplot(df, plt, fig_dir)
    generate_success_rate_by_category_count(df, plt, fig_dir)
    
    print(f"Charts saved in {fig_dir}")

def generate_charts():
    """Find all CSV files in results directory and subdirectories, and generate charts for each."""
    csv_files = glob.glob("results/**/*.csv", recursive=True)
    
    if not csv_files:
        print("No CSV files found in results directory")
        return
    
    print(f"Found {len(csv_files)} CSV file(s) to process")
    
    for csv_file in csv_files:
        try:
            process_csv_file(csv_file)
        except Exception as e:
            print(f"Error processing {csv_file}: {e}")
            continue
    
    print(f"\n{'='*60}")
    print("All charts generated successfully!")
    print(f"{'='*60}")

generate_charts()
