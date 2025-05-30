import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import glob
import os
import json

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
    plt.savefig("fig/success_rate_by_test_case.png")
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
    plt.savefig("fig/per_row_checkmarks.png")
    plt.close()

def generate_aggregated_plot(df, plot):
    summary = (
        df.groupby("expression-type")
        .agg(
            success_rate=("success", "mean"),
            std_dev=("success", "std"),
            avg_attempts=("attempts", "mean"),
            count=("success", "size")
        )
        .reset_index()
    )
    with open("settings.json", "r") as f:
        config = json.load(f)
    limit = config.get("agent-limit", "N/A")  # Use "N/A" if limit not found
    # Create custom x labels: "category (count)"
    summary["label"] = summary["expression-type"] + " (" + summary["count"].astype(str) + ")"

    # Calculate 95% CI
    z = 1.96  # for 95% CI
    p = summary["success_rate"]
    n = summary["count"]
    ci = z * np.sqrt((p * (1 - p)) / n)
    summary["ci"] = ci

    # Sort by success_rate
    summary = summary.sort_values("success_rate", ascending=False).reset_index(drop=True)

    # Set style
    sns.set(style="whitegrid")

    # Plot
    plt.figure(figsize=(6, 6))
    ax = sns.barplot(
        data=summary,
        x="label",
        y="success_rate",
        palette="Blues_d",
        width=0.7  # Reduce bar width
    )

    # Add error bars manually
    ax.errorbar(
        x=range(len(summary)),
        y=summary["success_rate"],
        yerr=summary["ci"],
        fmt='none',
        ecolor='orange',
        capsize=5,
        linewidth=1.3
    )

    # Annotate bars
    for i, row in summary.iterrows():
        ax.text(i, row.success_rate + 0.01, f"{row.success_rate:.2f}", ha='center', va='bottom', fontsize=9)

    # Labels and formatting
    plt.title(f"Success Rate by Linguistic Category (Max attempts = {limit})", fontsize=14)
    plt.xlabel("Linguistic Category (Number of Examples)", fontsize=12)
    plt.ylabel("Average Success Rate (CI)", fontsize=12)
    plt.ylim(0, 1.2)
    plt.xticks(rotation=45, ha="right")

    # Final layout
    plt.tight_layout()
    plt.savefig("fig/success_rate_by_category.png")
    plt.close()

#Average on the totals of the runs (prob. in the settings).

def generate_charts():
    latest_csv = get_latest_csv("logs/")
    df = pd.read_csv(latest_csv, delimiter=';', quotechar='"', encoding='utf-8')
    df["success"] = df["generated-expression"].notna().astype(int)
    df["attempts"] = pd.to_numeric(df["attempts"], errors="coerce")
    df["duration(ms)"] = pd.to_numeric(df["duration(ms)"], errors="coerce")
    df["test-case-short"] = df["test-case"].apply(
        lambda x: os.path.join(os.path.basename(os.path.dirname(str(x))), os.path.basename(str(x)))
    )
    sns.set_style("whitegrid")
    generate_success_rate_test_case_plot(df, plt)
    generate_summary_test_case_plot(df, plt)
    generate_aggregated_plot(df, plt)

generate_charts()
