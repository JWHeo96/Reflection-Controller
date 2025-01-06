package com.cos.reflect.controller.dto;

// 필요한 값만 전달하기 위한 클래스(어느 한 값에도 null이 있으면 안되기 대문에 model.User 사용 안하는 것이고 Dto 사용)
public class LoginDto {
	private String username;
	private String password;
	
	@Override
	public String toString() {
		return "LoginDto [username=" + username + ", password=" + password + "]";
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
}
