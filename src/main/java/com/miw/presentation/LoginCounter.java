package com.miw.presentation;

public class LoginCounter {

	private Integer logins;

	public Integer getLogins() {
		return this.logins;
	}
	
	public void inc()
	{
		this.logins ++;
	}
	
	public LoginCounter()
	{
		System.out.println("Inicializando el contador");
		this.logins=0;
	}
}
