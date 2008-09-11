package org.codehaus.groovy.grails.plugins.jcr.binding

import java.sql.Timestamp
import javax.jcr.Value
import javax.jcr.ValueFactory
import org.springframework.beans.TypeConverter
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder

/**
 * TODO: write javadoc
 *
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 */
class JcrValueConverter {

    static final BASE_CLASSES = [
            String, InputStream,
            Integer, int,
            Long, long,
            Float, float,
            Double, double,
            Boolean, boolean,
            Calendar
    ]

    TypeConverter typeConverter
    ValueFactory valueFactory

    public JcrValueConverter(TypeConverter typeConverter, ValueFactory valueFactory) {
        this.typeConverter = typeConverter
        this.valueFactory = valueFactory
    }

    Value convertToJcr(Object value) {
        Class sourceClass = value.getClass();
        if(BASE_CLASSES.find {Class clazz -> clazz.isAssignableFrom(sourceClass)}) {
            return valueFactory.createValue(value)
        } else if(Date.isAssignableFrom(sourceClass)) {
            return valueFactory.createValue(getLocalizedCalendarInstance(((Date) value).getTime()));
        } else if(sourceClass.isArray() && sourceClass.getComponentType() == byte.class) {
            return valueFactory.createValue(new ByteArrayInputStream((byte[]) value));
        } else {
            return valueFactory.createValue(String.valueOf(value))
        }
    }

    Object convertToJava(Value value, Class requiredClass) {
        if(requiredClass == Date.class) {
            return value.getDate().getTime();
        } else if(requiredClass == Timestamp) {
            return new Timestamp(value.getDate().getTimeInMillis());
        } else if(requiredClass == Calendar) {
            return value.getDate();
        } else if(requiredClass == InputStream) {
            return value.getStream();
        } else if(requiredClass.isArray() && requiredClass.getComponentType() == byte) {
            return readBytes(value.getStream());
        } else if(requiredClass == Integer || requiredClass == int) {
            return (int) value.getLong();
        } else if(requiredClass == Long || requiredClass == long) {
            return value.getLong();
        } else if(requiredClass == Float || requiredClass == float) {
            return (float) value.getDouble();
        } else if(requiredClass == Double || requiredClass == double) {
            return value.getDouble();
        } else if(requiredClass == Boolean || requiredClass == boolean) {
            return value.getBoolean();
        } else if(requiredClass.isEnum()) {
            return Enum.valueOf(requiredClass, value.getString());
        } else {
            return value.getString()
        }
    }

    byte[] readBytes(InputStream input) throws IOException {
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

    Calendar getLocalizedCalendarInstance(long millis) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.getRequestAttributes();
        Calendar calendar = webRequest != null ? Calendar.getInstance(webRequest.getLocale()) : Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }
}