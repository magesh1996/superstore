package com.superstore.chatbot.service;

// import java.util.ArrayList;
// import java.util.stream.Collectors;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
// import org.springframework.ai.chat.messages.SystemMessage;
// import org.springframework.ai.chat.messages.UserMessage;
// import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
// import org.springframework.ai.chat.messages.Message;
// import org.springframework.ai.chat.messages.SystemMessage;
// import org.springframework.ai.chat.messages.UserMessage;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.ai.chat.prompt.Prompt;
// import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
// import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
// import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatbotService {

    private final ChatClient chatClient;

    // private final ChatModel chatModel;
    // private final VectorStore vectorStore;
    // private final ChatMemory chatMemory;

    public ChatbotService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ToolCallbackProvider mcpToolCallbackProvider) {
    // public ChatbotService(ChatModel chatModel, VectorStore vectorStore) {

        // this.chatModel = chatModel;
        // this.vectorStore = vectorStore;

        // handle chat memory manually (retains up to 20 past messages)
        // this.chatMemory = MessageWindowChatMemory.builder()
        //     .maxMessages(20)
        //     .build();
        
        // system message instructs Llama 3.2 to act strictly as superstore assistant            
        
        String systemPrompt = """
            you are chatbot, the store operator-facing assistant for superstore.
            keep answers concise and conversational, as if replying in a live chat.
            only discuss superstore products, orders, and store policies; politely decline unrelated requests.
            you have access to product lookup tools via MCP.
            use them to fetch exact, real-time product details, stock, or search results when needed.
            """;
        
        String qaTemplate = """            
            use the context below for general store knowledge and policy questions.
            for exact product details, prices, or live data lookups, invoke the available product tools.
            this is india based store, so do not reformat currency code, recalculate, or convert them.
            if the context and product tools doesn't contain enough detail to answer confidently, 
            say so plainly and offer to help with something else.

            context:
            ---------------------
            {question_answer_context}
            ---------------------

            question: {query}
            """;        
        
        // configure custom vector store search parameters to retrieve more relevant product chunks
        SearchRequest searchRequest = SearchRequest.builder()
            .topK(6)                        // fetch top 6 chunks instead of default (prevents missing products)
            .similarityThreshold(0.5)  // slightly more forgiving similarity matching
            .build();

        // use MessageWindowChatMemory instead of InMemoryChatMemory
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(20) // retains up to 20 past messages
            .build();

        // main ChatClient instance with system prompt, memory advisor, and QA advisor
        this.chatClient = chatClientBuilder
            .defaultSystem(systemPrompt)
            .defaultOptions(OllamaChatOptions.builder().numCtx(2048))
            // register MCP server tools directly with the ChatClient
            .defaultTools(mcpToolCallbackProvider)
            // maintain conversation memory per user/session
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())            
            // automatically searches PGVector for relevant documents and attaches to prompt
            .defaultAdvisors(
                QuestionAnswerAdvisor
                    .builder(vectorStore)
                    .searchRequest(searchRequest)
                    .promptTemplate(PromptTemplate.builder().template(qaTemplate).build())
                    .build()
                )
            .build();

        // test 1 : no advisors at all
        // this.chatClient = chatClientBuilder.defaultSystem(systemPrompt).build();

        // test 2 : memory advisor only
        // this.chatClient = chatClientBuilder.defaultSystem(systemPrompt)
        //     .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        //     .build();

        // test 3 : QA advisor only
        // this.chatClient = chatClientBuilder.defaultSystem(systemPrompt)
        //     .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build())
        //     .build();
    }

    public Flux<String> askChatbot(String userConversationId, String userMessage) {
        return this.chatClient.prompt()
            .user(userMessage)
            // pass the conversation ID to keep context active across messages
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, userConversationId))            
            // .call()
            .stream() // stream token chunks from Ollama live
            .content();
    }

    // @SuppressWarnings("null")
    // public Flux<String> askChatbot(String userConversationId, String userMessageText) {
        
    //     // manual RAG : search PGVector for relevant document chunks
    //     SearchRequest searchRequest = SearchRequest.builder()
    //         .query(userMessageText)
    //         .topK(6)                        // fetch top 6 chunks
    //         .similarityThreshold(0.5)  // similarity threshold
    //         .build();
        
    //     List<Document> docs = this.vectorStore.similaritySearch(searchRequest);
        
    //     // extract text from the retrieved documents
    //     String context = docs.stream()
    //         .map(Document::getText)
    //         .collect(Collectors.joining("\n\n"));

    //     // construct the explicit system prompt injecting the context
    //     String systemPromptTemplate = """
    //         You are superbot, the customer-facing assistant for superstore.

    //         Answer using, in order of priority:
    //         1. The retrieved catalog context below
    //         2. The conversation history, for continuity and follow-up questions
    //         Do not use outside knowledge or make assumptions beyond what is given.

    //         If the catalog context and history don't contain enough detail to answer confidently,
    //         say so plainly and offer to help with something else. Never guess or invent product
    //         details, prices, or specifications.

    //         When quoting a price, copy it exactly as written in the context — do not reformat,
    //         recalculate, or convert it.

    //         Keep answers concise and conversational, as if replying in a live chat. Only discuss
    //         superstore products, orders, and store policies; politely decline unrelated requests.

    //         Retrieved Context from Catalog:
    //         ---------------------
    //         %s
    //         ---------------------
    //         """.formatted(context);

    //     // gather the entire message list for the prompt context window
    //     List<Message> messageList = new ArrayList<>();
        
    //     // add the dynamic system instructions first
    //     messageList.add(new SystemMessage(systemPromptTemplate));
        
    //     // fetch and append past history for this specific conversation ID
    //     List<Message> history = this.chatMemory.get(userConversationId);
    //     messageList.addAll(history);
        
    //     // add the current user query
    //     UserMessage currentUserMessage = new UserMessage(userMessageText);
    //     messageList.add(currentUserMessage);

    //     // package into a Prompt and stream token chunks
    //     Prompt prompt = new Prompt(messageList);

    //     // track the full response string dynamically to save it to memory later
    //     StringBuilder fullAssistantResponse = new StringBuilder();

    //     return this.chatModel.stream(prompt)
    //         .map(chatResponse -> {
    //             if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
    //                 String chunk = chatResponse.getResult().getOutput().getText();
    //                 if (chunk != null) {
    //                     fullAssistantResponse.append(chunk);
    //                     return chunk;
    //                 }
    //             }
    //             return "";
    //         })
    //         .doOnComplete(() -> {
    //             // manual memory sync : save both the current question and full response back to memory
    //             this.chatMemory.add(userConversationId, List.of(currentUserMessage));
    //             this.chatMemory.add(userConversationId, List.of(new UserMessage(fullAssistantResponse.toString()))); 
    //             // note: Spring AI tracks memory via message roles internally.
    //         });
    // }
}