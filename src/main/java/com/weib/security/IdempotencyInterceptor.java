package com.weib.security;
import com.weib.entity.User;import jakarta.servlet.http.*;import lombok.RequiredArgsConstructor;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.stereotype.Component;import org.springframework.web.method.HandlerMethod;import org.springframework.web.servlet.HandlerInterceptor;import java.io.IOException;
@Component @RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {
 private static final String ATTR=IdempotencyInterceptor.class.getName()+".context";private final IdempotencyService service;
 @Override public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler)throws IOException{
  if(!(handler instanceof HandlerMethod hm)||hm.getMethodAnnotation(Idempotent.class)==null)return true;
  String key=req.getHeader("Idempotency-Key");if(key==null||key.isBlank())key=req.getParameter("_idempotencyKey");
  if(key==null||key.isBlank()){write(res,400,"缺少幂等键",false);return false;}
  String scope=identity(req)+":"+req.getMethod()+":"+req.getRequestURI();
  IdempotencyService.State state;try{state=service.acquire(scope,key);}catch(IllegalArgumentException e){write(res,400,e.getMessage(),false);return false;}
  if(state==IdempotencyService.State.PROCESSING){write(res,409,"操作处理中，请勿重复提交",false);return false;}
  if(state==IdempotencyService.State.COMPLETED){write(res,200,"操作已完成",true);return false;}
  if(state==IdempotencyService.State.ACQUIRED)req.setAttribute(ATTR,new String[]{scope,key});return true;
 }
 @Override public void afterCompletion(HttpServletRequest req,HttpServletResponse res,Object h,Exception ex){Object v=req.getAttribute(ATTR);if(v instanceof String[] c){if(ex==null&&res.getStatus()<400)service.complete(c[0],c[1]);else service.release(c[0],c[1]);}}
 private String identity(HttpServletRequest req){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null&&auth.isAuthenticated()&&!"anonymousUser".equals(auth.getPrincipal()))return "u"+auth.getPrincipal();HttpSession s=req.getSession(false);if(s!=null){Object u=s.getAttribute("user");if(u instanceof User user&&user.getId()!=null)return "u"+user.getId();return "s"+s.getId();}return "ip"+req.getRemoteAddr();}
 private void write(HttpServletResponse r,int status,String msg,boolean dup)throws IOException{r.setStatus(status);r.setContentType("application/json;charset=UTF-8");r.getWriter().write("{\"code\":"+status+",\"message\":\""+msg+"\",\"duplicate\":"+dup+"}");}
}