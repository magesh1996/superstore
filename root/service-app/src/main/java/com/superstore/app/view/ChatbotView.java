package com.superstore.app.view;

import com.superstore.app.facade.ChatbotFacade;
import com.vaadin.flow.component.UI;
// import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInputI18n;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

import io.netty.handler.timeout.ReadTimeoutException;
import jakarta.annotation.security.PermitAll;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
// import java.util.UUID;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Route("chatbot")
// @PageTitle("chatbot")
@Menu(order = 4, title = "chatbot")
@PermitAll
// @CssImport("./styles/chatbot.css") // for frontend/ folder structure, use @CssImport instead of @StyleSheet
@StyleSheet("chatbot.css") // only loads when the user opens the /chatbot route
public class ChatbotView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotView.class);

    private final ChatbotFacade chatbotFacade;
    private final String conversationId;
    private final List<MessageListItem> messageItems = new ArrayList<>();
    private final MessageList messageList = new MessageList();

    public ChatbotView(ChatbotFacade chatbotFacade) {
        this.chatbotFacade = chatbotFacade;
        this.conversationId = UUID.randomUUID().toString();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // H3 title = new H3("superstore AI assistant");

        messageList.setSizeFull();
        // messageList.setMaxWidth("850px");

        MessageInput input = new MessageInput();
        input.setWidth("50%");
        
        MessageInputI18n i18n = new MessageInputI18n();
        i18n.setMessage("type a message...");
        i18n.setSend("send");
        
        input.setI18n(i18n);
        input.addSubmitListener(this::handleSendMessage);

        String botMessage = "hello! how can i help you today?";
        appendMessage(botMessage, "bot", "bot-message");

        add(messageList, input);
        
        setHorizontalComponentAlignment(Alignment.CENTER, messageList, input);
        expand(messageList);
    }

    private void handleSendMessage(MessageInput.SubmitEvent event) {
        
        String userMessage = event.getValue();
        // render user message right aligned
        appendMessage(userMessage, "you", "user-message");

        // // String botResponseText = "dummy response"; // chatbotFacade.askChatbot(conversationId, userMessage);
        // String botMessage = chatbotFacade.askChatbot(conversationId, userMessage);
        // // render bot response left aligned
        // appendMessage(botMessage, "bot", "bot-message");

        // create an empty bot message item to stream tokens into live
        MessageListItem botMessage = new MessageListItem("", Instant.now(), "bot");
        botMessage.addClassNames("bot-message");
        messageItems.add(botMessage);
        messageList.setItems(messageItems);

        // capture the current Vaadin UI instance for background thread access
        UI ui = UI.getCurrent();

        // stream tokens from the Flux
        chatbotFacade.askChatbot(conversationId, userMessage)
            .doOnNext(t -> System.out.println(System.currentTimeMillis() + " TOKEN: [" + t + "]"))
            .timeout(Duration.ofSeconds(120)) // Reactive safety timeout
            .onErrorResume(ReadTimeoutException.class, ex -> {
                logger.error("chatbot request timed out waiting for response.");
                return Mono.just("the request timed out. please try again.");
            })
            .onErrorResume(WebClientRequestException.class, ex -> {
                logger.error("Could not reach chatbot service at localhost:9000", ex);
                return Mono.just("the chatbot service is unavailable, please try again later.");
            })
            .subscribe(token -> {
                // Vaadin requires UI.access() when updating UI elements asynchronously
                ui.access(() -> {
                    String currentText = botMessage.getText();
                    botMessage.setText(currentText + token);
                    // trigger UI re-render for messageList updates
                    messageList.setItems(new ArrayList<>(messageItems));
                });
            });
    }

    private void appendMessage(String text, String userName, String className) {
        MessageListItem message = new MessageListItem(text, Instant.now(), userName);
        message.addClassNames(className);
        messageItems.add(message);
        messageList.setItems(messageItems);
    }
}