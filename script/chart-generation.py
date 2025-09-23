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

def generate_summary_test_case_plot(df, plot):
    df_plot = df[::-1].reset_index(drop=True)
    row_height = 0.5
    plt.figure(figsize=(10, len(df_plot) * row_height))

    for idx, row in df_plot.iterrows():
        is_success = not pd.isna(row["generated-expression"])
        color = "green" if is_success else "red"
        marker_text = f"{'OK' if is_success else 'KO'}"
        plt.text(0.15, idx, marker_text, ha="left", va="center", fontsize=10.5, color=color)

    plt.yticks(range(len(df_plot)), df_plot["test-case-short"], fontsize=8)
    plt.xticks([])
    plt.gca().spines[['top', 'right', 'left', 'bottom']].set_visible(False)
    plt.grid(False)
    plt.tick_params(left=False)
    plt.title("Test Outcome per Test Case (OK = Success, KO = Failure). Max Attempts = 4", pad=20)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/per_row_checkmarks.png")
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

    plt.title("Success Rate by Linguistic Category (split by target-value)")
    plt.xlabel("Category")
    plt.ylabel("Average Success Rate")
    plt.xticks(rotation=45, ha="right")
    plt.ylim(0,1.1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_category.png")
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

    summary["label"] = summary["category_count"].astype(str) + " (" + summary["count"].astype(str) + ")"

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

    plt.title("Success Rate by Number of Categories (grouped by target-value)")
    plt.xlabel("Number of Categories (Number of Examples)")
    plt.ylabel("Average Success Rate")
    plt.xticks(rotation=45, ha="right")
    plt.ylim(0, 1.1)
    plt.tight_layout()
    plt.savefig(f"{fig_dir}/success_rate_by_category_count.png")
    plt.close()

def generate_charts():
    latest_csv = "logs/log_1758489858583.csv"
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
    generate_summary_test_case_plot(df, plt)
    generate_aggregated_plot(df, plt)
    generate_success_rate_by_category_count(df, plt)

generate_charts()
