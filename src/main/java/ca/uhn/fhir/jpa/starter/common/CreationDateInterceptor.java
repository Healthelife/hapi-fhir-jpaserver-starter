package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Interceptor
public class CreationDateInterceptor {

    private static final Logger ourLog =
            LoggerFactory.getLogger(CreationDateInterceptor.class);

    public static final String CREATION_DATE_EXTENSION_URL =
            "http://abdm.gov.in/fhir/StructureDefinition/creation-date";

    /**
     * Add creation-date extension during CREATE
     */
    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
    public void handleCreate(IBaseResource resource) {

        if (!(resource instanceof DomainResource)) {
            return;
        }

        DomainResource domainResource =
                (DomainResource) resource;

        if (hasCreationDate(domainResource)) {
            return;
        }

        Extension extension = new Extension();

        extension.setUrl(CREATION_DATE_EXTENSION_URL);

        extension.setValue(
                new DateTimeType(new Date()));

        domainResource.addExtension(extension);

        ourLog.debug(
                "Stamped creation date on {}",
                domainResource.getResourceType().name());
    }

    /**
     * Preserve original creation-date extension
     * during PUT and PATCH
     */
    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
    public void handleUpdate(
            IBaseResource oldResource,
            IBaseResource newResource) {

        if (!(oldResource instanceof DomainResource)
                || !(newResource instanceof DomainResource)) {
            return;
        }

        DomainResource existingResource =
                (DomainResource) oldResource;

        DomainResource updatedResource =
                (DomainResource) newResource;

        try {

            /*
             * Remove any incoming creation-date
             * extension from client request
             */
            updatedResource.getExtension().removeIf(
                    ext -> CREATION_DATE_EXTENSION_URL.equals(
                            ext.getUrl()));

            /*
             * Restore original extension
             */
            Extension existingExtension =
                    existingResource.getExtensionByUrl(
                            CREATION_DATE_EXTENSION_URL);

            if (existingExtension != null) {

                updatedResource.addExtension(
                        existingExtension.copy());

                ourLog.debug(
                        "Preserved creation-date extension on {}",
                        updatedResource.getResourceType().name());
            }

        } catch (Exception e) {

            ourLog.error(
                    "Error preserving creation-date extension",
                    e);
        }
    }

    private boolean hasCreationDate(
            DomainResource resource) {

        if (resource == null
                || resource.getExtension() == null) {
            return false;
        }

        for (Extension extension : resource.getExtension()) {

            if (CREATION_DATE_EXTENSION_URL.equals(
                    extension.getUrl())) {

                return true;
            }
        }

        return false;
    }
}