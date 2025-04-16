import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import glob
import os

def get_latest_csv(folder_path):
    csv_files = glob.glob(os.path.join(folder_path, "*.csv"))

    if not csv_files:
        exit(1)

    latest_file = max(csv_files, key=os.path.getmtime)
    return latest_file

folder = "logs/"
latest_csv = get_latest_csv(folder)
print(latest_csv)
df = pd.read_csv(latest_csv, delimiter=';', quotechar='"', encoding='utf-8')
df["success"] = df["generated-expression"].notna().astype(int)
df["attempts"] = pd.to_numeric(df["attempts"], errors="coerce")
df["duration(ms)"] = pd.to_numeric(df["duration(ms)"], errors="coerce")
df["test-case-short"] = df["test-case"].apply(
    lambda x: os.path.join(os.path.basename(os.path.dirname(str(x))), os.path.basename(str(x)))
)

sns.set_style("whitegrid")

# Plot 1: Total Success vs. Failure
plt.figure(figsize=(6, 4))
sns.countplot(x=df["success"].map({1: "Success", 0: "Failure"}), palette=["green", "red"])
plt.title("Generated Expression Distribution")
plt.xlabel("Test Outcome")
plt.ylabel("Count")
plt.savefig("fig/success_failure.png")
plt.close()

#Chart 2
#Scatter plot attempts / duration

plt.figure(figsize=(8, 5))
sns.scatterplot(data=df, x="attempts", y="duration(ms)", hue="success", palette={1: "green", 0: "red"}, alpha=0.7)
plt.title("Attempts vs Duration")
plt.xlabel("Number of Attempts")
plt.ylabel("Duration (ms)")
plt.legend(title="Success")
plt.tight_layout()
plt.savefig("fig/attempts_vs_duration.png")
plt.close()

#Plot 3 - Success Rate per Test Case across the variants

summary = (
    df.groupby("test-case-short")
    .agg(success_rate=("success", "mean"), avg_attempts=("attempts", "mean"), num_queries=("test-case-short", "count"))
    .reset_index()
)
summary["y_label"] = summary["test-case-short"] + " (" + summary["num_queries"].astype(str) + " queries)"

plt.figure(figsize=(10, 6))
ax = sns.barplot(data=summary, x="success_rate", y="y_label")

# Enable this to show the average of attempts
# for i, row in summary.iterrows():
#     ax.text(
#         row["success_rate"] + 0.02,
#         i,
#         f"{row['avg_attempts']:.2f} mean attempts",
#         va="center"
#     )

plt.title("Success Rate per Test Case (Max attempts = 4)")
plt.xlabel("Success Rate")
plt.ylabel("Test Case")
plt.xlim(0, 1)
plt.tight_layout()
plt.savefig("fig/success_rate_by_test_case.png")
plt.close()

#Plot 4 - Result for each testcases
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
