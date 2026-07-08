package com.weib.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CredentialPolicyTest {
 @Test void usernameBoundaries(){assertNotNull(CredentialPolicy.validateUsername("ab"));assertNull(CredentialPolicy.validateUsername("abc"));assertNull(CredentialPolicy.validateUsername("a".repeat(32)));assertNotNull(CredentialPolicy.validateUsername("a".repeat(33)));}
 @Test void usernameCharacters(){assertNull(CredentialPolicy.validateUsername("测试_user1"));assertNotNull(CredentialPolicy.validateUsername("bad-name"));}
 @Test void passwordBoundariesAndClasses(){assertNotNull(CredentialPolicy.validatePassword("Aa12345"));assertNull(CredentialPolicy.validatePassword("Aa123456"));assertNull(CredentialPolicy.validatePassword("Aa1"+"x".repeat(61)));assertNotNull(CredentialPolicy.validatePassword("Aa1"+"x".repeat(62)));assertNotNull(CredentialPolicy.validatePassword("abcdefgh1"));assertNotNull(CredentialPolicy.validatePassword("ABCDEFGH1"));assertNotNull(CredentialPolicy.validatePassword("Abcdefgh"));}
 @Test void passwordCannotEqualIdentity(){assertNotNull(CredentialPolicy.validatePassword("User123A","User123A",null));assertNotNull(CredentialPolicy.validatePassword("A13800138000","user","A13800138000"));}
 @Test void loginInputsAreBounded(){assertFalse(CredentialPolicy.validLoginInput("u".repeat(33),"Aa123456"));assertFalse(CredentialPolicy.validLoginInput("user","A".repeat(65)));assertTrue(CredentialPolicy.validLoginInput("user","Aa123456"));}
}