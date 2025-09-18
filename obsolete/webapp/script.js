let fileHandles = [];
let jsonData = {};

// Open modal to load JSON
document.getElementById('openJson').addEventListener('click', () => {
    const modal = new bootstrap.Modal(document.getElementById('jsonModal'));
    modal.show();
});

// Load JSON and fetch files
document.getElementById('loadJsonButton').addEventListener('click', () => {
    try {
        const input = document.getElementById('jsonInput').value;
        jsonData = JSON.parse(input);

        document.getElementById('systemPrompt').value = jsonData.system_prompt;
        loadFilesFromPath(jsonData.learningCasesPath);
        bootstrap.Modal.getInstance(document.getElementById('jsonModal')).hide();
    } catch (error) {
        alert('Invalid JSON format');
    }
});

// Save system prompt
document.getElementById('saveSystemPrompt').addEventListener('click', () => {
    jsonData.system_prompt = document.getElementById('systemPrompt').value;
    alert('System Prompt updated successfully');
});

// Load files from a given path
async function loadFilesFromPath(path) {
    try {
        const directoryHandle = await window.showDirectoryPicker();
        fileHandles = [];

        const fileList = document.getElementById('fileItems');
        fileList.innerHTML = '';

        for await (const entry of directoryHandle.values()) {
            if (entry.kind === 'file') {
                fileHandles.push(entry);

                const listItem = document.createElement('li');
                listItem.textContent = entry.name;
                listItem.className = 'list-group-item';
                listItem.addEventListener('click', () => onFileClick(entry, listItem));
                fileList.appendChild(listItem);
            }
        }
    } catch (error) {
        console.error('Error accessing directory:', error);
    }
}

// Handle file click
async function onFileClick(fileHandle, listItem) {
    try {
        // Highlight active file
        document.querySelectorAll('.sidebar li').forEach(li => li.classList.remove('active'));
        listItem.classList.add('active');

        const file = await fileHandle.getFile();
        const content = JSON.parse(await file.text());

        displayEditor(content, fileHandle);
    } catch (error) {
        console.error('Error reading file:', error);
    }
}

// Display editor with file content
function displayEditor(content, fileHandle) {
    document.getElementById('editor').classList.remove('hidden');

    document.getElementById('variables').value = JSON.stringify(content.variables, null, 2);
    document.getElementById('datasets').value = JSON.stringify(content.datasets, null, 2);
    document.getElementById('imports').value = JSON.stringify(content.imports, null, 2);
    document.getElementById('expected').value = content.expected;
    document.getElementById('code').value = content.code;
    document.getElementById('text').value = content.text;

    document.getElementById('saveFile').onclick = () => saveFile(fileHandle);
}

// Save changes to file
async function saveFile(fileHandle) {
    try {
        const content = {
            variables: JSON.parse(document.getElementById('variables').value),
            datasets: JSON.parse(document.getElementById('datasets').value),
            imports: JSON.parse(document.getElementById('imports').value),
            expected: document.getElementById('expected').value,
            code: document.getElementById('code').value,
            text: document.getElementById('text').value
        };

        const writable = await fileHandle.createWritable();
        await writable.write(JSON.stringify(content, null, 2));
        await writable.close();

        alert('File saved successfully');
    } catch (error) {
        console.error('Error saving file:', error);
    }
}