package net.symphonious.longpoll;

public class TestValue extends AbstractSequencedEvent
{
    private StringBuffer value;

    public TestValue(final String value)
    {
        this.value = new StringBuffer(value);
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

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final TestValue testValue = (TestValue) o;

        if (value != null ? !value.equals(testValue.value) : testValue.value != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return value != null ? value.hashCode() : 0;
    }
}
