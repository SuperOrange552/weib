(function () {
  function csrfToken() {
    return document.querySelector('meta[name="csrf-token"]')?.content || '';
  }

  function esc(value) {
    return String(value || '').replace(/[&<>"']/g, function (ch) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch];
    });
  }

  window.openComplaintModal = function (targetType, targetId) {
    var old = document.getElementById('weibComplaintModal');
    if (old) old.remove();
    var modal = document.createElement('div');
    modal.id = 'weibComplaintModal';
    modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.6);z-index:9999;display:flex;align-items:center;justify-content:center;padding:20px';
    modal.innerHTML = '<div style="width:min(520px,100%);background:#fff;border-radius:14px;padding:24px;color:#222">' +
      '<h3 style="margin:0 0 16px">投诉内容</h3>' +
      '<p style="font-size:13px;color:#777">对象：' + esc(targetType) + ' #' + esc(targetId) + '</p>' +
      '<select id="weibComplaintCategory" style="width:100%;padding:10px;margin:8px 0"><option value="FAKE_JOB">虚假职位</option><option value="FAKE_PHOTO">虚假照片/资料</option><option value="FRAUD">诈骗或收费</option><option value="HARASSMENT">骚扰或不当联系</option><option value="SPAM">垃圾信息</option><option value="ILLEGAL">违法违规</option><option value="OTHER">其他</option></select>' +
      '<textarea id="weibComplaintDescription" rows="5" maxlength="2000" placeholder="请填写具体情况（2-2000字）" style="width:100%;padding:10px;box-sizing:border-box"></textarea>' +
      '<textarea id="weibComplaintEvidence" rows="3" placeholder="证据地址，每行一个，最多5个（/uploads/... 或 http(s)://）" style="width:100%;padding:10px;box-sizing:border-box;margin-top:8px"></textarea>' +
      '<div id="weibComplaintError" style="color:#d33;font-size:13px;min-height:20px;margin-top:8px"></div>' +
      '<div style="display:flex;justify-content:flex-end;gap:10px;margin-top:12px"><button type="button" id="weibComplaintCancel">取消</button><button type="button" id="weibComplaintSubmit" style="background:#1677ff;color:#fff;border:0;border-radius:6px;padding:8px 18px">提交投诉</button></div>' +
      '</div>';
    document.body.appendChild(modal);
    document.getElementById('weibComplaintCancel').onclick = function () { modal.remove(); };
    document.getElementById('weibComplaintSubmit').onclick = async function () {
      var submit = this;
      var desc = document.getElementById('weibComplaintDescription').value.trim();
      var evidence = document.getElementById('weibComplaintEvidence').value.split(/\r?\n/).map(function (v) { return v.trim(); }).filter(Boolean);
      submit.disabled = true;
      document.getElementById('weibComplaintError').textContent = '';
      try {
        var response = await fetch('/api/complaints', { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken() }, body: JSON.stringify({ targetType: targetType, targetId: Number(targetId), category: document.getElementById('weibComplaintCategory').value, description: desc, evidenceUrls: evidence }) });
        var data = await response.json().catch(function () { return {}; });
        if (!response.ok || data.code !== 200) throw new Error(data.msg || '投诉提交失败');
        alert('投诉已提交，编号：' + (data.data?.id || '已受理'));
        modal.remove();
      } catch (e) {
        document.getElementById('weibComplaintError').textContent = e.message || '投诉提交失败';
        submit.disabled = false;
      }
    };
  };
})();
