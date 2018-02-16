/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.worker.core.mapping;

import java.util.List;

import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.model.unit.CustodialHistoryModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.TextByLang;

/**
 * Map the object DescriptiveMetadataContentType generated by jaxb when parse manifest.xml
 * To a local java object DescriptiveMetadataModel that should match Unit data base model
 */
public class DescriptiveMetadataMapper {

    /**
     * element Mapper.
     */
    private ElementMapper elementMapper;

    /**
     * CustodialHistory mapper
     */
    private CustodialHistoryMapper custodialHistoryMapper;

    /**
     * constructor
     */
    public DescriptiveMetadataMapper() {
        this.elementMapper = new ElementMapper();
        this.custodialHistoryMapper = new CustodialHistoryMapper();
    }

    /**
     * Map jaxb DescriptiveMetadataContentType to local DescriptiveMetadataModel
     *
     * @param metadataContentType JAXB Object
     * @return DescriptiveMetadataModel
     */
    public DescriptiveMetadataModel map(DescriptiveMetadataContentType metadataContentType) {

        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();
        descriptiveMetadataModel.setAcquiredDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getAcquiredDate()));
        descriptiveMetadataModel.getAddressee().addAll(metadataContentType.getAddressee());

        descriptiveMetadataModel.setAny(elementMapper.toMap(metadataContentType.getAny()));
        descriptiveMetadataModel
            .setArchivalAgencyArchiveUnitIdentifier(metadataContentType.getArchivalAgencyArchiveUnitIdentifier());

        descriptiveMetadataModel.setAuthorizedAgent(metadataContentType.getAuthorizedAgent());

        descriptiveMetadataModel.setCoverage(metadataContentType.getCoverage());
        descriptiveMetadataModel.setCreatedDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getCreatedDate()));

        CustodialHistoryModel custodialHistoryModel =
            custodialHistoryMapper.map(metadataContentType.getCustodialHistory());
        descriptiveMetadataModel.setCustodialHistory(custodialHistoryModel);

        descriptiveMetadataModel.setDescription(findDefaultTextType(metadataContentType.getDescription()));
        TextByLang description_ = new TextByLang(metadataContentType.getDescription());

        if (description_.isNotEmpty()) {
            descriptiveMetadataModel.setDescription_(description_);
        }

        descriptiveMetadataModel.setDescriptionLanguage(metadataContentType.getDescriptionLanguage());
        descriptiveMetadataModel.setDescriptionLevel(metadataContentType.getDescriptionLevel());
        descriptiveMetadataModel.setDocumentType(metadataContentType.getDocumentType());
        descriptiveMetadataModel.setEndDate(metadataContentType.getEndDate());
        descriptiveMetadataModel.setEvent(metadataContentType.getEvent());
        descriptiveMetadataModel.setFilePlanPosition(metadataContentType.getFilePlanPosition());
        descriptiveMetadataModel.setGps(metadataContentType.getGps());
        descriptiveMetadataModel.setHref(metadataContentType.getHref());
        descriptiveMetadataModel.setId(metadataContentType.getId());
        descriptiveMetadataModel.setKeyword(metadataContentType.getKeyword());
        descriptiveMetadataModel.setLanguage(metadataContentType.getLanguage());
        descriptiveMetadataModel.setOriginatingAgency(metadataContentType.getOriginatingAgency());
        descriptiveMetadataModel
            .setOriginatingAgencyArchiveUnitIdentifier(metadataContentType.getOriginatingAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setOriginatingSystemId(metadataContentType.getOriginatingSystemId());
        descriptiveMetadataModel.getRecipient().addAll(metadataContentType.getRecipient());

        descriptiveMetadataModel.setRegisteredDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getRegisteredDate()));
        descriptiveMetadataModel.setRelatedObjectReference(metadataContentType.getRelatedObjectReference());
        descriptiveMetadataModel.setRestrictionEndDate(metadataContentType.getRestrictionEndDate());
        descriptiveMetadataModel.setRestrictionRuleIdRef(metadataContentType.getRestrictionRuleIdRef());
        descriptiveMetadataModel.setRestrictionValue(metadataContentType.getRestrictionValue());
        descriptiveMetadataModel.setReceivedDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getReceivedDate()));
        descriptiveMetadataModel.setSentDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getSentDate()));

        descriptiveMetadataModel.setSignature(metadataContentType.getSignature());

        descriptiveMetadataModel.setSource(metadataContentType.getSource());
        descriptiveMetadataModel.setStartDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getStartDate()));
        descriptiveMetadataModel.setStatus(metadataContentType.getStatus());
        descriptiveMetadataModel.setSubmissionAgency(metadataContentType.getSubmissionAgency());
        descriptiveMetadataModel.setSystemId(metadataContentType.getSystemId());
        descriptiveMetadataModel.setTag(metadataContentType.getTag());

        descriptiveMetadataModel.setTitle(findDefaultTextType(metadataContentType.getTitle()));
        TextByLang title_ = new TextByLang(metadataContentType.getTitle());
        if (title_.isNotEmpty()) {
            descriptiveMetadataModel.setTitle_(title_);
        }

        descriptiveMetadataModel.setTransactedDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getTransactedDate()));
        descriptiveMetadataModel.setTransferringAgencyArchiveUnitIdentifier(
            metadataContentType.getTransferringAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setType(metadataContentType.getType());
        descriptiveMetadataModel.setVersion(metadataContentType.getVersion());
        descriptiveMetadataModel.setWriter(metadataContentType.getWriter());

        return descriptiveMetadataModel;
    }

    public String findDefaultTextType(List<TextType> textTypes) {
        return textTypes.stream()
            .filter(t -> t.getLang() == null)
            .findFirst()
            .map(TextType::getValue).orElse(null);
    }

}
