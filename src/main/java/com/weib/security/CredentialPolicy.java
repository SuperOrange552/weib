package com.weib.security;

public final class CredentialPolicy {
 public static final int USERNAME_MIN=3, USERNAME_MAX=32, PASSWORD_MIN=8, PASSWORD_MAX=64;
 private CredentialPolicy(){}
 public static String normalizeUsername(String value){return value==null?null:value.trim();}
 public static String validateUsername(String value){
  String v=normalizeUsername(value);
  if(v==null||v.isEmpty())return "用户名不能为空";
  if(v.length()<USERNAME_MIN||v.length()>USERNAME_MAX)return "用户名长度必须为3-32位";
  if(!v.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$"))return "用户名只能包含字母、数字、下划线和中文";
  return null;
 }
 public static String validatePassword(String password){return validatePassword(password,null,null);}
 public static String validatePassword(String password,String username,String phone){
  if(password==null||password.length()<PASSWORD_MIN)return "密码长度至少8位";
  if(password.length()>PASSWORD_MAX)return "密码长度不能超过64位";
  if(!password.matches(".*[a-z].*"))return "密码必须包含小写字母";
  if(!password.matches(".*[A-Z].*"))return "密码必须包含大写字母";
  if(!password.matches(".*\\d.*"))return "密码必须包含数字";
  if(username!=null&&password.equalsIgnoreCase(username.trim()))return "密码不能与用户名相同";
  if(phone!=null&&password.equals(phone.trim()))return "密码不能与手机号相同";
  return null;
 }
 public static boolean validLoginInput(String username,String password){
  if(username==null||password==null)return false;
  int ul=username.trim().length(),pl=password.length();
  return ul>=1&&ul<=USERNAME_MAX&&pl>=1&&pl<=PASSWORD_MAX;
 }
}