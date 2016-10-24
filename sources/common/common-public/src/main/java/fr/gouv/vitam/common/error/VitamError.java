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

package fr.gouv.vitam.common.error;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;


/**
 * VitamError class
 *
 */
public class VitamError {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamError.class);
    
    @JsonProperty("httpCode")
    private int httpCode;
    @JsonProperty("code")
    private String code;
    @JsonProperty("context")
    private String context;
    @JsonProperty("state")
    private String state;
    @JsonProperty("message")
    private String message;
    @JsonProperty("description")
    private String description;
    @JsonProperty("errors")
    private List<VitamError> errors;

    protected VitamError() {
        // For Json builder
    }

    /**
     * RequestResponseError constructor
     *
     * @param code the code used to identify this error object
     **/
    public VitamError(String code) {
        this.code = code;
        errors = new ArrayList<>();
    }

    /**
     * @param code of error as integer
     * @return the VitamError object with the code is setted
     */
    public VitamError setCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * @return the httpCode
     */
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * @param httpCode the httpCode to set
     *
     * @return this
     */
    public VitamError setHttpCode(int httpCode) {
        this.httpCode = httpCode;
        return this;
    }

    /**
     * @param context of error as String
     * @return the VitamError object with the context is setted
     */
    public VitamError setContext(String context) {
        ParametersChecker.checkParameter("context is a mandatory parameter", context);
        this.context = context;
        return this;
    }

    /**
     * @param state of error as String
     * @return the VitamError object with the error state is setted
     */
    public VitamError setState(String state) {
        ParametersChecker.checkParameter("state is a mandatory parameter", state);
        this.state = state;
        return this;
    }

    /**
     * @param message of error as String
     * @return the VitamError object with the error message is setted
     */
    public VitamError setMessage(String message) {
        ParametersChecker.checkParameter("message is a mandatory parameter", message);
        this.message = message;
        return this;
    }

    /**
     * @param description of error as String
     * @return the VitamError object with the description error is setted
     */
    public VitamError setDescription(String description) {
        ParametersChecker.checkParameter("description is a mandatory parameter", description);
        this.description = description;
        return this;
    }

    /**
     * @param errors errors as List
     * @return the VitamError object with the list of errors is setted
     */
    public VitamError setErrors(List<VitamError> errors) {
        ParametersChecker.checkParameter("errors list is a mandatory parameter", errors);
        if (this.errors == null) {
            this.errors = errors;
        } else {
            this.errors.addAll(errors);
        }
        return this;
    }

    /**
     * @return the code of the VitamError object
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the context of the VitamError object
     */
    public String getContext() {
        return context;
    }

    /**
     * @return the state of the VitamError object
     */
    public String getState() {
        return state;
    }

    /**
     * @return the message of the VitamError object
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the description of the VitamError object
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the errors list of the VitamError object
     */
    public List<VitamError> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrintLowerCamelCase(this);
    }
    
    /**
     * 
     * @return the Json representation
     * @throws InvalidParseOperationException 
     */
    public JsonNode toJsonNode() {
        try {
            return JsonHandler.getFromString(this.toString());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * 
     * @param string
     * @return the corresponding VitamError
     * @throws InvalidParseOperationException
     */
    public static VitamError getFromString(String string) throws InvalidParseOperationException {
        return JsonHandler.getFromStringLowerCamelCase(string, VitamError.class);
    }

    /**
     * 
     * @param node
     * @return the corresponding VitamError
     * @throws InvalidParseOperationException
     */
    public static VitamError getFromJsonNode(JsonNode node) throws InvalidParseOperationException {
        return JsonHandler.getFromJsonNodeLowerCamelCase(node, VitamError.class);
    }
}
