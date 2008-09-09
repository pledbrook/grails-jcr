package org.codehaus.groovy.grails.plugins.jcr.binding

import javax.jcr.RepositoryException
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import java.sql.Timestamp
import javax.jcr.Value
import javax.jcr.ValueFactory
import org.springframework.web.context.request.RequestContextHolder

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class JcrHelper {

    static Object getOptimalValue(Value value, Class targetClass) {
        if(targetClass == String.class) {
            return value.getString();
        } else if(targetClass == Date.class) {
            return value.getDate().getTime();
        } else if(targetClass == Timestamp.class) {
            return new Timestamp(value.getDate().getTimeInMillis());
        } else if(targetClass == Calendar.class) {
            return value.getDate();
        } else if(targetClass == InputStream.class) {
            return value.getStream();
        } else if(targetClass.isArray() && targetClass.getComponentType() == byte.class) {
            // byte array...we need to read from the stream
            return readBytes(value.getStream());
        } else if(targetClass == Integer.class || targetClass == int.class) {
            return (int) value.getDouble();
        } else if(targetClass == Long.class || targetClass == long.class) {
            return value.getLong();
        } else if(targetClass == Double.class || targetClass == double.class) {
            return value.getDouble();
        } else if(targetClass == Boolean.class || targetClass == boolean.class) {
            return value.getBoolean();
        } else if(targetClass.isEnum()) {
            return Enum.valueOf(targetClass, value.getString());
        } else {
            return value.getString()
        }
    }

    /**
     * Converts gives object to appropriate JCR Value instance.
     */
    static Value createValue(Object fieldValue, ValueFactory valueFactory) {
        Class sourceClass = fieldValue.getClass();
        if(sourceClass == String.class) {
            return valueFactory.createValue((String) fieldValue);
        } else if(Date.isAssignableFrom(sourceClass)) {
            return valueFactory.createValue(getLocalizedCalendarInstance(((Date) fieldValue).getTime()));
        } else if(Calendar.isAssignableFrom(sourceClass)) {
            return valueFactory.createValue((Calendar) fieldValue);
        } else if(sourceClass == InputStream.class) {
            return valueFactory.createValue((InputStream) fieldValue);
        } else if(sourceClass.isArray() && sourceClass.getComponentType() == byte.class) {
            return valueFactory.createValue(new ByteArrayInputStream((byte[]) fieldValue));
        } else if(sourceClass == Integer.class || sourceClass == int.class) {
            return valueFactory.createValue((Integer) fieldValue);
        } else if(sourceClass == Long.class || sourceClass == long.class) {
            return valueFactory.createValue((Long) fieldValue);
        } else if(sourceClass == Double.class || sourceClass == double.class) {
            return valueFactory.createValue((Double) fieldValue);
        } else if(sourceClass == Boolean.class || sourceClass == boolean.class) {
            return valueFactory.createValue((Boolean) fieldValue);
        } else if(sourceClass == Locale.class) {
            return valueFactory.createValue(String.valueOf((Locale) fieldValue));
        } else if(sourceClass.isEnum()) {
            return valueFactory.createValue(String.valueOf(fieldValue))
        } else {
            return valueFactory.createValue(String.valueOf(fieldValue))
        }
    }

    static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while((len = input.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            input.close();
            out.close();
        }
        return out.toByteArray();
    }

    static Calendar getLocalizedCalendarInstance(long millis) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        Calendar calendar = webRequest != null ? Calendar.getInstance(webRequest.getLocale()) : Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }
}