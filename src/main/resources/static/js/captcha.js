(() => {
  function init() {
    const user=document.querySelector('input[name="username"]');
    const pass=document.querySelector('input[name="password"]');
    const answer=document.querySelector('input[name="captcha"]');
    const image=document.getElementById('captchaImg');
    const status=document.getElementById('captchaStatus');
    if (!user||!pass||!answer||!image||!status) return;
    let timer=null, deadline=0, objectUrl=null, loading=false, lastRefresh=0, initialized=false;
    const ready=()=>user.value.trim()!==''&&pass.value!=='';
    const stop=()=>{ if(timer){clearInterval(timer);timer=null;} };
    const format=s=>`${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
    const tick=()=>{
      if(!ready()){ stop(); image.classList.add('disabled'); status.textContent='请先输入账号和密码'; return; }
      const left=Math.max(0,Math.ceil((deadline-Date.now())/1000)); status.textContent=`有效期 ${format(left)}`;
      if(left===0){ stop(); refresh(true); }
    };
    async function refresh(automatic=false){
      if(!ready()){status.textContent='请先输入账号和密码';return;}
      if(loading)return;
      const wait=Math.ceil((5000-(Date.now()-lastRefresh))/1000);
      if(!automatic&&wait>0){status.textContent=`请在 ${wait} 秒后刷新`;return;}
      loading=true;
      try{
        const response=await fetch('/captcha',{cache:'no-store'});
        if(!response.ok){const data=await response.json().catch(()=>({}));status.textContent=data.message||'获取验证码失败';return;}
        const blob=await response.blob(); if(objectUrl)URL.revokeObjectURL(objectUrl); objectUrl=URL.createObjectURL(blob);
        image.src=objectUrl; image.classList.remove('disabled'); answer.value=''; lastRefresh=Date.now();
        deadline=Date.now()+(Number(response.headers.get('X-Captcha-Expires-In'))||120)*1000;
        stop(); tick(); timer=setInterval(tick,1000); initialized=true;
      } catch(e){status.textContent='验证码加载失败，请稍后重试';} finally{loading=false;}
    }
    const fieldsChanged=()=>{ if(ready()&&!initialized)refresh(true); else if(!ready()){initialized=false;stop();image.removeAttribute('src');status.textContent='请先输入账号和密码';} };
    user.addEventListener('input',fieldsChanged);pass.addEventListener('input',fieldsChanged);image.addEventListener('click',()=>refresh(false));
    window.addEventListener('beforeunload',()=>{stop();if(objectUrl)URL.revokeObjectURL(objectUrl);}); fieldsChanged();
  }
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',init):init();
})();