package com.proofpoint.event.monitor;

public interface Alerter
{
    public void failed(String name, String description);
    public void recovered(String name, String description);
}
