package org.fao.geonet.services.metadata.format;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import org.eclipse.jetty.util.IO;
import org.fao.geonet.constants.Params;
import org.fao.geonet.domain.MetadataType;
import org.fao.geonet.domain.ReservedGroup;
import org.fao.geonet.kernel.GeonetworkDataDirectory;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.services.AbstractServiceIntegrationTest;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.List;
import javax.annotation.Nullable;

import static org.fao.geonet.domain.Pair.read;
import static org.junit.Assert.assertFalse;

public class FormatIntegrationTest extends AbstractServiceIntegrationTest {

    @Autowired
    private GeonetworkDataDirectory dataDirectory;
    @Autowired
    private SchemaManager schemaManager;
    private ServiceConfig serviceConfig;

    @Before
    public void setUp() throws Exception {
        this.serviceConfig = new ServiceConfig();
        serviceConfig.setValue(FormatterConstants.USER_XSL_DIR, dataDirectory.getFormatterDir().getAbsolutePath());
    }

    @Test
    public void testExec() throws Exception {
        final ServiceContext serviceContext = createServiceContext();
        loginAsAdmin(serviceContext);

        final Element sampleMetadataXml = getSampleMetadataXml();
        final ByteArrayInputStream stream = new ByteArrayInputStream(Xml.getString(sampleMetadataXml).getBytes("UTF-8"));
        final int id =  importMetadataXML(serviceContext, "uuid", stream, MetadataType.METADATA,
                ReservedGroup.all.getId(), Params.GENERATE_UUID);
        final String schema = schemaManager.autodetectSchema(sampleMetadataXml);

        final ListFormatters listService = new ListFormatters();

        listService.init(dataDirectory.getWebappDir(), serviceConfig);
        final Element formattersEl = listService.exec(createParams(read("schema", schema)), serviceContext);

        final List<String> formatters = Lists.transform(formattersEl.getChildren("formatter"), new Function() {
            @Nullable
            @Override
            public String apply(@Nullable Object input) {
                return ((Element)input).getText();
            }
        });

        for (String formatter : formatters) {
            final Format formatService = new Format();
            formatService.init(dataDirectory.getWebappDir(), serviceConfig);
            final Element view = formatService.exec(createParams(read("id", id), read("xsl", formatter)), serviceContext);
            view.setName("body");
            Element html = new Element("html").addContent(view);
            assertFalse(html.getChildren().isEmpty());
        }
    }

    @Test
    public void testExecXslt() throws Exception {
        final ServiceContext serviceContext = createServiceContext();
        loginAsAdmin(serviceContext);

        final Element sampleMetadataXml = getSampleMetadataXml();
        final ByteArrayInputStream stream = new ByteArrayInputStream(Xml.getString(sampleMetadataXml).getBytes("UTF-8"));
        final int id =  importMetadataXML(serviceContext, "uuid", stream, MetadataType.METADATA,
                ReservedGroup.all.getId(), Params.GENERATE_UUID);
        final String formatterName = "xsl-test-formatter";
        final URL testFormatterViewFile = FormatIntegrationTest.class.getResource(formatterName+"/view.xsl");
        final File testFormatter = new File(testFormatterViewFile.getFile()).getParentFile();
        IO.copy(testFormatter, new File(this.dataDirectory.getFormatterDir(), formatterName));
        final String functionsXslName = "functions.xsl";
        IO.copy(new File(testFormatter.getParentFile(), functionsXslName), new File(this.dataDirectory.getFormatterDir(), functionsXslName));

        final Format formatService = new Format();
        formatService.init(dataDirectory.getWebappDir(), serviceConfig);
        final Element view = formatService.exec(createParams(read("id", id), read("xsl", formatterName)), serviceContext);

        assertEqualsText("fromFunction", view, "*//p");
    }

}