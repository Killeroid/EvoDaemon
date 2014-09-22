package EvoDaemon.Utils;

import java.lang.reflect.Method;

public class LoadClass {

	static ClassLoader scl = ClassLoader.getSystemClassLoader();

	/*
	 * Check if class is loaded by systemloader
	 */
	public static boolean isClassLoaded(String className) 
			throws Exception {
		Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
		m.setAccessible(true);
		if (m.invoke(scl, new Object[] { className }) == null) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * Define class from byetcode
	 */
	public static synchronized Class<?> defineClass(byte[] bytecode)
			throws Exception {
		Class<?>[] types = new Class<?>[] {
				String.class, byte[].class, int.class, int.class
		};
		Object[] args = new Object[] {
				null, bytecode, 0, bytecode.length
		};
		Method m = ClassLoader.class.getDeclaredMethod("defineClass", types);
		m.setAccessible(true);
		return (Class<?>) m.invoke(scl, args);
	}

	/*
	 * Load class if not already loaded
	 */
	public static synchronized Class<?> loadClass(String className)
			throws Exception {
		Class<?>[] types = new Class<?>[] {
				String.class, boolean.class
		};
		Object[] args = new Object[] {
				className, true 
		};
		Method m = ClassLoader.class.getDeclaredMethod("loadClass", types);
		m.setAccessible(true);
		return (Class<?>) m.invoke(scl, args);
	}

	/*
	 * Load class if not already loaded
	 */
	public static Class<?> loadClass(ClassLoader loader, String className)
			throws Exception {
		//ClassLoader scl = ClassLoader.getSystemClassLoader();
		synchronized(loader) {
			Class<?>[] types = new Class<?>[] {
					String.class, boolean.class
			};
			Object[] args = new Object[] {
					className, true 
			};
			Method m = loader.getClass().getDeclaredMethod("loadClass", types);
			m.setAccessible(true);
			return (Class<?>) m.invoke(loader, args);
		}

	}

}