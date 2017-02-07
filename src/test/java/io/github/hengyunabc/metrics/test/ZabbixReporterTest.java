package io.github.hengyunabc.metrics.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.hengyunabc.metrics.ZabbixReporter;
import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.SenderResult;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;

/**
 *
 */
@RunWith(MockitoJUnitRunner.Strict.class)
public class ZabbixReporterTest {

    @Mock
    private ZabbixSender zabbixSender;

    private ZabbixReporter instance;

    private MetricRegistry metricRegistry = new MetricRegistry();

    @Before
    public void setup()
    {
        instance = ZabbixReporter.forRegistry(metricRegistry)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.MILLISECONDS)
                .hostName("hostname")
                .prefix("prefix.")
                .suffix("[suffix]")
                .name("junittest")
                .build(zabbixSender);
    }

    @Test
    public void should_gauge_report_the_value() throws IOException
    {
        // Given
        SenderResult senderResult = new SenderResult();
        senderResult.setProcessed(1);
        senderResult.setFailed(0);
        doReturn(senderResult).when(zabbixSender).send(any(List.class), anyLong());

        metricRegistry.register("gauge", new Gauge<Long>() {
            @Override
            public Long getValue()
            {
                return 1L;
            }
        });
        // When
        instance.report();

        // Then
        ArgumentCaptor<List<DataObject>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(zabbixSender).send(captor.capture(), anyLong());
        assertThat(captor.getValue()).hasSize(1);
        final DataObject dataObject = captor.getValue().get(0);
        assertThat(dataObject.getHost()).isEqualTo("hostname");
        assertThat(dataObject.getKey()).isEqualTo("prefix.gauge[suffix]");
        assertThat(dataObject.getValue()).isEqualTo("1");
    }
}
