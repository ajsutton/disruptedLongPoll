package net.symphonious.longpoll;

public class TestValue
{
    private StringBuilder value;

    public TestValue(final String value)
    {
        this.value = new StringBuilder(value);
    }

    public String getValue()
    {
        return value.toString();
    }

    public void append(final String valueToAppend)
    {
        value.append(valueToAppend);
    }

    public void setValue(final String newValue)
    {
        value.setLength(0);
        value.append(newValue);
    }
}
