import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import glob
import os
import json

fig_dir = "paper/fig"

def get_latest_csv(folder_path):
    csv_files = glob.glob(os.path.join(folder_path, "*.csv"))

    if not csv_files:
        exit(1)

    latest_file = max(csv_files, key=os.path.getmtime)
    print(f"Using latest CSV file: {latest_file}")
    return latest_file

def generate_success_rate_test_case_plot(df, plot):
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

def generate_aggregated_plot(df, plot):
    # esplodo le categorie
    df['expression-type'] = df['expression-type'].str.strip('[]').str.split(',')
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

def generate_aggregated_boxplot(df, plot):
    df['expression-type'] = df['expression-type'].str.strip('[]').str.split(',')
    df_exploded = df.explode('expression-type')
    df_exploded['expression-type'] = df_exploded['expression-type'].str.strip()

    summary = (
        df_exploded.groupby(["runId", "expression-type", "target-value"])
        .agg(success_rate=("success", "mean"))
        .reset_index()
    )

    category_counts = (
        df_exploded.groupby("expression-type")
        .apply(lambda x: x[['test-case', 'target-value']].drop_duplicates().shape[0])
        .to_dict()
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

    categories = sorted(summary['expression-type'].unique())
    target_values = sorted(summary['target-value'].unique())
    
    for i, category in enumerate(categories):
        for j, target_val in enumerate(target_values):
            count = df_exploded[
                (df_exploded['expression-type'] == category) & 
                (df_exploded['target-value'] == target_val)
            ].shape[0] // df_exploded['runId'].nunique()
            
            if count > 0:
                x_offset = -0.2 if j == 0 else 0.2
                mask = (summary['expression-type'] == category) & (summary['target-value'] == target_val)
                if mask.sum() > 0:
                    median_y = summary[mask]['success_rate'].median()
                else:
                    median_y = 0.5  
                
                ax.text(i + x_offset, median_y, f'n:{count}', 
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


def generate_success_rate_by_category_count(df, plot):
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
    plt.ylabel("Average Success Rate over 5 runs")
    plt.xticks(rotation=45, ha="right")
    plt.ylim(0, 1.1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_complexity.png")
    plt.close()

def generate_charts():
    latest_csv = get_latest_csv("logs")
    print(f"Using latest CSV file: {latest_csv}")
    df = pd.read_csv(latest_csv, delimiter=';', quotechar='"', encoding='utf-8')
    df["success"] = df["generated-expression"].notna().astype(int)
    df["target-value"] = df["target-value"].astype(int)
    df["attempts"] = pd.to_numeric(df["attempts"], errors="coerce")
    df["duration(ms)"] = pd.to_numeric(df["duration(ms)"], errors="coerce")
    df["test-case-short"] = df["test-case"].apply(
        lambda x: os.path.join(os.path.basename(os.path.dirname(str(x))), os.path.basename(str(x)))
    )

    sns.set_style("whitegrid")
    generate_success_rate_test_case_plot(df, plt)
    #generate_aggregated_plot(df, plt)
    generate_aggregated_boxplot(df, plt)
    print("Generated charts saved in", fig_dir)
    generate_success_rate_by_category_count(df, plt)

generate_charts()
