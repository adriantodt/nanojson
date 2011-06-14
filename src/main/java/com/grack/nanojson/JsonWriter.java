package com.grack.nanojson;

import java.io.IOException;

/**
 * {@link JsonWriter} is a more structured JSON writer that uses generics to
 * ensure that you can't write invalid JSON. This class is useful for writing
 * JSON with well-known structure.
 * 
 * Caveat: because of its use of generics, it cannot be used to write JSON where
 * the object/array nesting structure is not known at compile time.
 * 
 * This class wraps a {@link JsonEmitter} internally.
 */
public class JsonWriter {
	private interface Context {
	}

	private interface RootContext extends Context, Appendable {
	}

	/**
	 * The context used when writing to a {@link String}.
	 */
	public interface RootStringContext extends RootContext {
		String end();
	}

	/**
	 * This context is used at the top level of the {@link JsonWriter}. A single
	 * value may be written to it.
	 */
	public interface RootValueContext<T extends RootContext> extends Context {
		/**
		 * Writes a {@link String} value, then ends this JSON value.
		 */
		T value(String s);

		/**
		 * Writes an {@link Integer} value, then ends this JSON value.
		 */
		T value(int i);

		/**
		 * Writes a {@link Boolean} value, then ends this JSON value.
		 */
		T value(boolean b);

		/**
		 * Writes a {@link Double} value, then ends this JSON value.
		 */
		T value(double d);

		/**
		 * Starts writing the root array.
		 */
		ArrayContext<T> array();

		/**
		 * Starts writing the root object.
		 */
		ObjectContext<T> object();
	}

	/**
	 * This context is used when the values inside of an array are being
	 * written.
	 */
	public interface ArrayContext<T extends Context> extends Context {
		/**
		 * Writes a {@link String} to this array.
		 */
		ArrayContext<T> value(String s);

		/**
		 * Writes an {@link Integer} to this array.
		 */
		ArrayContext<T> value(int i);

		/**
		 * Writes a {@link Boolean} to this array.
		 */
		ArrayContext<T> value(boolean b);

		/**
		 * Writes a {@link Double} to this array.
		 */
		ArrayContext<T> value(double d);

		/**
		 * Starts writing a nested array inside of this array.
		 */
		ArrayContext<ArrayContext<T>> array();

		/**
		 * Starts writing a nested object inside of this array.
		 */
		ObjectContext<ArrayContext<T>> object();

		/**
		 * Ends this array and moves back to the parent JSON array or object.
		 */
		T end();
	}

	/**
	 * This context is used when the key/value pairs of a JSON object are being
	 * written.
	 */
	public interface ObjectContext<T extends Context> extends Context {
		/**
		 * Writes a {@link String} to this object.
		 */
		ObjectContext<T> value(String key, String s);

		/**
		 * Writes an {@link Integer} to this object.
		 */
		ObjectContext<T> value(String key, int i);

		/**
		 * Writes a {@link Boolean} to this object.
		 */
		ObjectContext<T> value(String key, boolean b);

		/**
		 * Writes a {@link Double} to this object.
		 */
		ObjectContext<T> value(String key, double d);

		/**
		 * Starts writing a nested array inside of this object.
		 */
		ArrayContext<ObjectContext<T>> array(String key);

		/**
		 * Starts writing a nested object inside of this object.
		 */
		ObjectContext<ObjectContext<T>> object(String key);

		/**
		 * Ends this array and moves back to the parent JSON array or object.
		 */
		T end();
	}

	/**
	 * Implementation of {@link Context} for writing to a {@link String}.
	 */
	private static class RootStringContextImpl implements RootStringContext {
		private StringBuilder builder = new StringBuilder();

		private RootStringContextImpl() {
		}

		public String end() {
			return builder.toString();
		}

		@Override
		public Appendable append(CharSequence csq) throws IOException {
			return builder.append(csq);
		}

		@Override
		public Appendable append(char c) throws IOException {
			return builder.append(c);
		}

		@Override
		public Appendable append(CharSequence csq, int start, int end)
				throws IOException {
			return builder.append(csq, start, end);
		}
	}

	/**
	 * Implementation for the various emit methods. Generics handle the
	 * specialization of this class into {@link RootValueContext},
	 * {@link ObjectContext} and {@link ArrayContext}.
	 */
	private abstract static class ContextImpl<T extends Context> implements
			Context {
		protected final JsonEmitter emitter;
		private final T t;

		/**
		 * Workaround for inability to pass "this" to a superconstructor.
		 */
		@SuppressWarnings("unchecked")
		public ContextImpl(JsonEmitter emitter) {
			this.t = (T) this;
			this.emitter = emitter;
		}

		public ContextImpl(T t, JsonEmitter emitter) {
			this.t = t;
			this.emitter = emitter;
		}

		public T value(String s) {
			emitter.value(s);
			return t;
		}

		public T value(int i) {
			emitter.value(i);
			return t;
		}

		public T value(boolean b) {
			emitter.value(b);
			return t;
		}

		public T value(double d) {
			emitter.value(d);
			return t;
		}

		public T value(String key, String s) {
			emitter.value(key, s);
			return t;
		}

		public T value(String key, int i) {
			emitter.value(key, i);
			return t;
		}

		public T value(String key, boolean b) {
			emitter.value(key, b);
			return t;
		}

		public T value(String key, double d) {
			emitter.value(key, d);
			return t;
		}

		public ArrayContext<T> array() {
			emitter.startArray();
			return new ArrayContextImpl<T>(t, emitter);
		}

		public ObjectContext<T> object() {
			emitter.startObject();
			return new ObjectContextImpl<T>(t, emitter);
		}

		public ArrayContext<T> array(String key) {
			emitter.startArray(key);
			return new ArrayContextImpl<T>(t, emitter);
		}

		public ObjectContext<T> object(String key) {
			emitter.startObject(key);
			return new ObjectContextImpl<T>(t, emitter);
		}
	}

	private static class RootValueContextImpl<T extends RootContext> extends
			ContextImpl<T> implements RootValueContext<T> {
		public RootValueContextImpl(T t) {
			super(t, new JsonEmitter(t));
		}
	}

	private static class ArrayContextImpl<T extends Context> extends
			ContextImpl<ArrayContext<T>> implements ArrayContext<T> {
		private final T t;

		public ArrayContextImpl(T t, JsonEmitter emitter) {
			super(emitter);
			this.t = t;
		}

		@Override
		public T end() {
			emitter.endArray();
			return t;
		}
	}

	private static class ObjectContextImpl<T extends Context> extends
			ContextImpl<ObjectContext<T>> implements ObjectContext<T> {
		private final T t;

		public ObjectContextImpl(T t, JsonEmitter emitter) {
			super(emitter);
			this.t = t;
		}

		@Override
		public T end() {
			emitter.endObject();
			return t;
		}
	}

	/**
	 * Starts writing a {@link String}.
	 */
	public static RootValueContext<RootStringContext> string() {
		return new RootValueContextImpl<RootStringContext>(
				new RootStringContextImpl());
	}
}
