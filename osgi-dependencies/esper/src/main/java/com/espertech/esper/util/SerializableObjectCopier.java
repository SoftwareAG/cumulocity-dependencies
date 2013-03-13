package com.espertech.esper.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * Utility class for copying serializable objects via object input and output streams.
 */
public class SerializableObjectCopier {
    /**
     * Deep copies the input object.
     * @param orig is the object to be copied, must be serializable
     * @return copied object
     * @throws IOException if the streams returned an exception
     * @throws ClassNotFoundException if the de-serialize fails
     */
    public static Object copy(Object orig) throws IOException, ClassNotFoundException {
        SimpleByteArrayOutputStream fbos = new SimpleByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(fbos);
        out.writeObject(orig);
        out.flush();
        out.close();

        ObjectInputStream in = new ContextClassloaderAwareObjectInputStream(fbos.getInputStream());
        return in.readObject();
    }
    
    private static class ContextClassloaderAwareObjectInputStream extends ObjectInputStream {

        public ContextClassloaderAwareObjectInputStream(InputStream in) throws IOException {
            super(in);
        }
        
        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                try {
                    return Class.forName(desc.getName(), false, contextClassLoader);
                } catch (ClassNotFoundException e) {
                    // ignore and resolve using standard impl below
                }
            }
            return super.resolveClass(desc);
        }
    }
}
