(()=>{
 const uuid=()=>crypto.randomUUID?crypto.randomUUID():`${Date.now()}-${Math.random().toString(16).slice(2)}`;
 document.addEventListener('DOMContentLoaded',()=>{
  document.querySelectorAll('form').forEach(form=>{
   const method=(form.getAttribute('method')||'get').toLowerCase();if(method==='get')return;
   let key=form.querySelector('input[name="_idempotencyKey"]');if(!key){key=document.createElement('input');key.type='hidden';key.name='_idempotencyKey';key.value=uuid();form.appendChild(key);}
   form.addEventListener('submit',e=>{if(form.dataset.submitting==='1'){e.preventDefault();return;}form.dataset.submitting='1';form.querySelectorAll('button[type="submit"],input[type="submit"]').forEach(b=>{b.disabled=true;b.dataset.originalText=b.textContent||b.value;if(b.tagName==='BUTTON')b.textContent='处理中…';else b.value='处理中…';});});
  });
 });
 window.addEventListener('pageshow',()=>document.querySelectorAll('form[data-submitting="1"]').forEach(f=>{delete f.dataset.submitting;f.querySelectorAll('[data-original-text]').forEach(b=>{b.disabled=false;if(b.tagName==='BUTTON')b.textContent=b.dataset.originalText;else b.value=b.dataset.originalText;delete b.dataset.originalText;});}));
 const originalFetch=window.fetch.bind(window),pending=new Map();
 window.fetch=(input,init={})=>{const method=(init.method||'GET').toUpperCase();if(['GET','HEAD','OPTIONS'].includes(method))return originalFetch(input,init);const fingerprint=`${method}:${typeof input==='string'?input:input.url}:${typeof init.body==='string'?init.body:''}`;let key=pending.get(fingerprint);if(!key){key=uuid();pending.set(fingerprint,key);}const headers=new Headers(init.headers||{});if(!headers.has('Idempotency-Key'))headers.set('Idempotency-Key',key);return originalFetch(input,{...init,headers}).finally(()=>pending.delete(fingerprint));};
 window.newIdempotencyKey=uuid;
})();