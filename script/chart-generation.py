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

df = pd.read_csv(latest_csv, delimiter=';', quotechar='"', encoding='utf-8')

# Create 'success' column (1 if 'generated-expression' is not "NULL", 0 otherwise)
df["success"] = df["generated-expression"].notna().astype(int)


# Set style
sns.set_style("whitegrid")

# Plot 1: Success vs. Failure
plt.figure(figsize=(6, 4))
sns.countplot(x=df["success"].map({1: "Success", 0: "Failure"}), palette=["green", "red"])
plt.title("Generated Expression Distribution")
plt.xlabel("Test Outcome")
plt.ylabel("Count")
plt.savefig("logs/success_failure.png")
plt.close()

#Chart 2
#Scatter plot attempts / duration

df["attempts"] = pd.to_numeric(df["attempts"], errors="coerce")
df["duration(ms)"] = pd.to_numeric(df["duration(ms)"], errors="coerce")

plt.figure(figsize=(8, 5))
sns.scatterplot(data=df, x="attempts", y="duration(ms)", hue="success", palette={1: "green", 0: "red"}, alpha=0.7)
plt.title("Attempts vs Duration")
plt.xlabel("Number of Attempts")
plt.ylabel("Duration (ms)")
plt.legend(title="Success")
plt.tight_layout()
plt.savefig("logs/attempts_vs_duration.png")
plt.close()


