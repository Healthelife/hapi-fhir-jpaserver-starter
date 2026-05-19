package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Interceptor
public class PatchExtensionPreservingInterceptor {

    private static final Logger ourLog =
            LoggerFactory.getLogger(PatchExtensionPreservingInterceptor.class);

    public static final String CREATION_DATE_EXTENSION_URL =
            "http://abdm.gov.in/fhir/StructureDefinition/creation-date";

    @Autowired
    private DaoRegistry myDaoRegistry;

    /**
     * Executed before resource is stored after PATCH processing
     */
    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
    public void handlePatchedResource(
            IBaseResource oldResource,
            IBaseResource newResource,
            RequestDetails requestDetails) {

        if (!(oldResource instanceof DomainResource)
                || !(newResource instanceof DomainResource)) {
            return;
        }

        DomainResource existingResource =
                (DomainResource) oldResource;

        DomainResource patchedResource =
                (DomainResource) newResource;

        try {

            /*
             * Remove any creation-date extension
             * coming from PATCH payload
             */
            patchedResource.getExtension().removeIf(
                    ext -> CREATION_DATE_EXTENSION_URL.equals(ext.getUrl()));

            /*
             * Restore original creation-date extension
             * from stored resource
             */
            Extension existingExtension =
                    existingResource.getExtensionByUrl(
                            CREATION_DATE_EXTENSION_URL);

            if (existingExtension != null) {

                patchedResource.addExtension(
                        existingExtension.copy());

                ourLog.debug(
                        "Preserved original creation-date extension for PATCH on {}",
                        patchedResource.getResourceType().name());
            }

        } catch (Exception e) {

            ourLog.error(
                    "Error while preserving PATCH extensions",
                    e);
        }
    }
}