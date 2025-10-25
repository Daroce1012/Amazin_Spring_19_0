package com.miw.model;



import jakarta.validation.constraints.NotEmpty;

import org.hibernate.validator.constraints.Length;


public class LoginData {

	@NotEmpty
	private String login;
	@Length(min = 1, max = 8, message = "Debe tener entre 1 y 8 caracteres.")
	private String password;
	private String captcha;
	
	public String getCaptcha() {
		return captcha;
	}
	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	@Override
	public String toString() {
		return "LoginData [login=" + login + ", password=" + password + "]";
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}

