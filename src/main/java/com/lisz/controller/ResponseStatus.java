package com.lisz.controller;

public class ResponseStatus {
	private int code;
	private String message;
	private String data; // JSON
	
	public ResponseStatus() {}
	
	public ResponseStatus(int code, String message, String data) {
		super();
		this.code = code;
		this.message = message;
		this.data = data;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}

	public static ResponseStatus build(int i) {
		return new ResponseStatus(200, "OK", "Created an account successfully.");
	}
	
}
