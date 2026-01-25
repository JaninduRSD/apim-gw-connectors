package org.wso2.azure.gw.client.policy.policies;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.wso2.carbon.apimgt.api.APIManagementException;

public class AzureTopicSmartPolicy extends AzurePolicy{
    private String topicPath;
    private List<AzurePolicy> innerPolicies;

    public AzureTopicSmartPolicy(String topicPath) {
        // We don't set a specific AzurePolicyType enum here as this is a container/custom type
        this.topicPath = topicPath;
        this.innerPolicies = new ArrayList<>();
    }

    public void addInnerPolicy(AzurePolicy policy) {
        this.innerPolicies.add(policy);
    }

    @Override
    public void processDocument(DocumentBuilder documentBuilder) throws APIManagementException {
        // 1. Create a new Document to hold our XML structure
        Document doc = documentBuilder.newDocument();
        
        // 2. Create the root <choose> element
        Element chooseElement = doc.createElement("choose");
        
        // 3. Create the <when> element
        Element whenElement = doc.createElement("when");
        
        // 4. Set the Azure Policy Condition
        // Checks if the request URL path ends with the topic name (e.g. "/chat")
        String condition = String.format("@(context.Request.OriginalUrl.Path.EndsWith(\"%s\"))", topicPath);
        whenElement.setAttribute("condition", condition);

        // 5. Process Inner Policies and append them to <when>
        for (AzurePolicy policy : innerPolicies) {
            // Tell the inner policy to build its own XML structure first
            policy.processDocument(documentBuilder);
            
            // Get the root element of the inner policy
            Element policyRoot = policy.getRoot();
            
            if (policyRoot != null) {
                // Import the node into our current document so we can attach it
                Node importedNode = doc.importNode(policyRoot, true);
                whenElement.appendChild(importedNode);
            }
        }

        // 6. Assemble the structure
        chooseElement.appendChild(whenElement);
        
        // 7. CRITICAL: Set the root of this policy object so the Builder can find it
        this.setRoot(chooseElement);
    }
}
