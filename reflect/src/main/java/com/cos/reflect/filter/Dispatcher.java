package com.cos.reflect.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cos.reflect.anno.RequestMapping;
import com.cos.reflect.controller.UserController;

// 분기 시키기
public class Dispatcher implements Filter {
	
	private boolean isMatching = false;
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		
//		System.out.println("컨텍스트 패스 : " + request.getContextPath());
//		System.out.println("식별자 주소: " + request.getRequestURI());
//		System.out.println("전체 주소 : " + request.getRequestURL());
		
		// /user 파싱하기
		String endPoint = request.getRequestURI().replaceAll(request.getContextPath(), "");
		System.out.println("엔드포인트 : " + endPoint); // user/login
		
//		if (endPoint.equals("/join")) {
//			userController.join();
//		} else if (endPoint.equals("/login")) {
//			userController.login();
//		} else if (endPoint.equals("/user")) {
//			userController.user();
//		}
		
		UserController userController = new UserController();
		
		// 리플렉션 -> 메서드를 런타임 시점에서 찾아내서 실행
		Method[] methods = userController.getClass().getDeclaredMethods(); // 그 파일에 메서드만
//		for (Method method : methods) {
//			//System.out.println(method.getName());
//			
//			if (endPoint.equals("/" + method.getName())) {
//				try {
//					method.invoke(userController);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
		
		for (Method method : methods) { // 4바퀴 (join, login, user, hello)
			Annotation annotation = method.getDeclaredAnnotation(RequestMapping.class);
			RequestMapping requestMapping = (RequestMapping) annotation; // 다운캐스팅 (사용할 수 있는 메소드가 다름)
			// System.out.println(requestMapping.value());
			
			if (requestMapping.value().equals(endPoint)) {
				isMatching = true;
				try {
					// 파라미터 분석
					Parameter[] params = method.getParameters();
					String path = null;
					
					if (params.length != 0) {
						// System.out.println("params[0].getType() : " + params[0].getType());
						
						Class<?> paramType = params[0].getType();
						Object dtoInstance = paramType.getDeclaredConstructor().newInstance(); // 해당 오브젝트를 리플렉션해서 set함수 호출 (username, password)
						
//						String username = request.getParameter("username");
//						String password = request.getParameter("password");
//						System.out.println("username : " + username);
//						System.out.println("password : " + password);
						
						// key 값을 변형 username => setUsername
						// key 값을 변형 password => setPassword
						setData(dtoInstance, request); // return받지 않아도 됨 -> 레퍼런스 변수값의 주소를 넘겼기 때문에
						
						path = (String)method.invoke(userController, dtoInstance);
					} else {
						path = (String)method.invoke(userController);
					}
					
					RequestDispatcher dis = request.getRequestDispatcher(path); // 필터를 다시 안탐 (내부적으로 동작)
					//response.sendRedirect(endPoint); 필터를 탐
					dis.forward(request, response);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			} 
		}
		
		if (!isMatching) {
			response.setContentType("text/html; charset=utf-8");
			PrintWriter out = response.getWriter();
			out.print("잘못된 주소 요청입니다. 404 에러");
			out.flush();
		}
	}
	
	private <T>void setData(T instance, HttpServletRequest request) {
		Enumeration<String> keys = request.getParameterNames(); // 크기 : 2 (username, password)
		
		// 열거형 타입 -> 첫번째 주소를 알아야 두번째 주소를 찾을 수 있다.
		while(keys.hasMoreElements()) {
			String key = (String) keys.nextElement(); // username, password
			String methodKey = keyToMethodKey(key); // setUsername
			
			Method[] methods = instance.getClass().getDeclaredMethods(); // 5
			
			for (Method method : methods) {
				if (method.getName().equals(methodKey)) {
					
					Class<?> parameterType = method.getParameters()[0].getType();
					Object value = convertValueByType(request.getParameter(key), parameterType);
					
					try {
						method.invoke(instance, value);
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private String keyToMethodKey(String key) {
		String firstKey = "set";
		String upperKey = key.substring(0, 1).toUpperCase();
		String remainKey = key.substring(1);
		
		return firstKey + upperKey + remainKey;
	}
	
	private Object convertValueByType(Object value, Class<?> targetType) {
		if (value == null) {
			return null;
		}
		
		if (targetType == String.class) {
	        return value.toString(); // 모든 Object를 String으로 변환 가능
	    } else if (targetType == int.class || targetType == Integer.class) {
	        return Integer.parseInt(value.toString());
	    } else if (targetType == long.class || targetType == Long.class) {
	        return Long.parseLong(value.toString());
	    } else if (targetType == double.class || targetType == Double.class) {
	        return Double.parseDouble(value.toString());
	    } else if (targetType == boolean.class || targetType == Boolean.class) {
	        return Boolean.parseBoolean(value.toString());
	    } else if (targetType == float.class || targetType == Float.class) {
	        return Float.parseFloat(value.toString());
	    } else if (targetType == short.class || targetType == Short.class) {
	        return Short.parseShort(value.toString());
	    } else if (targetType == byte.class || targetType == Byte.class) {
	        return Byte.parseByte(value.toString());
	    } else {
	        throw new IllegalArgumentException("Unsupported parameter type: " + targetType.getName());
	    }
	}

	@Override
	public void destroy() {
		System.out.println("destroy()");
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("init()");
	}
}
