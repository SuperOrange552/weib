const csrf = document.querySelector('meta[name="csrf-token"]')?.content || '';
const form = document.getElementById('compose');
const fileInput = document.getElementById('images');
const upload = document.getElementById('upload');
const previews = document.getElementById('previews');
const message = document.getElementById('message');
let selectedFiles = [];

function showMessage(text) { message.textContent = text || ''; }

async function readJson(response) {
    const type = response.headers.get('content-type') || '';
    if (type.includes('application/json')) {
        try { return await response.json(); }
        catch (ignored) { return { code: response.status, msg: '服务器返回的数据格式不正确' }; }
    }
    if (response.status === 401 || response.status === 403) {
        return { code: response.status, msg: '登录状态已失效，请重新登录' };
    }
    if (response.status === 413) {
        return { code: response.status, msg: '图片超过服务器允许的上传大小（单张最大 10MB）' };
    }
    if (response.status === 429) {
        return { code: response.status, msg: '图片上传过于频繁，请稍后重试' };
    }
    return { code: response.status, msg: `请求失败（HTTP ${response.status}），请稍后重试` };
}

function addFiles(files) {
    const incoming = Array.from(files || []);
    const invalid = incoming.find(file => !/^image\/(png|jpe?g|gif|webp)$/i.test(file.type) || file.size > 10 * 1024 * 1024);
    if (invalid) { showMessage('只能上传 PNG、JPG、GIF 或 WEBP 图片，且单张不超过 10MB'); return; }
    selectedFiles = [...selectedFiles, ...incoming].slice(0, 9);
    renderPreviews();
}

function renderPreviews() {
    previews.innerHTML = '';
    selectedFiles.forEach((file, index) => {
        const item = document.createElement('div');
        item.className = 'forum-preview';
        const image = document.createElement('img');
        image.alt = file.name;
        image.src = URL.createObjectURL(file);
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.textContent = '×';
        remove.title = '移除图片';
        remove.onclick = () => { selectedFiles.splice(index, 1); renderPreviews(); };
        item.append(image, remove);
        previews.appendChild(item);
    });
}

upload.addEventListener('click', () => fileInput.click());
upload.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') fileInput.click(); });
fileInput.addEventListener('change', event => addFiles(event.target.files));
['dragenter', 'dragover'].forEach(type => upload.addEventListener(type, event => { event.preventDefault(); upload.classList.add('is-dragover'); }));
['dragleave', 'drop'].forEach(type => upload.addEventListener(type, event => { event.preventDefault(); upload.classList.remove('is-dragover'); }));
upload.addEventListener('drop', event => addFiles(event.dataTransfer.files));

async function loadSections() {
    const response = await fetch('/api/forum/sections');
    const result = await readJson(response);
    document.getElementById('sectionId').innerHTML = (result.data || []).map(section => `<option value="${section.id}">${section.name}</option>`).join('');
}

async function uploadImages() {
    const urls = [];
    for (const file of selectedFiles) {
        const body = new FormData();
        body.append('file', file);
        const response = await fetch('/api/forum/media', { method: 'POST', headers: { 'X-CSRF-Token': csrf }, body });
        const result = await readJson(response);
        if (!response.ok || !result.data?.url) throw new Error(`${file.name}：${result.msg || '图片上传失败'}`);
        urls.push(result.data.url);
    }
    return urls;
}

form.addEventListener('submit', async event => {
    event.preventDefault();
    showMessage('');
    const submit = form.querySelector('button[type="submit"]');
    submit.disabled = true;
    try {
        const imageUrls = await uploadImages();
        const body = {
            sectionId: Number(document.getElementById('sectionId').value),
            title: document.getElementById('title').value.trim(),
            content: document.getElementById('content').value.trim(),
            tags: document.getElementById('tags').value.split(',').map(tag => tag.trim()).filter(Boolean),
            imageUrls
        };
        const response = await fetch('/api/forum/posts', { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf }, body: JSON.stringify(body) });
        const result = await readJson(response);
        if (!response.ok || !result.data?.id) throw new Error(result.msg || '帖子发布失败，请稍后重试');
        location.href = '/forum/post/' + result.data.id;
    } catch (error) {
        showMessage(error.message || '操作失败，请稍后重试');
        submit.disabled = false;
    }
});

loadSections().catch(() => showMessage('板块加载失败，请刷新页面重试'));
