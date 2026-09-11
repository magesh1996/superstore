package com.superstore.chatbot.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.superstore.chatbot.record.TokenEvent;
import com.superstore.chatbot.service.ChatbotService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController
{
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // @GetMapping("/ask")                                                      // option 1 : returns all token chunks as a single String (sync)
    // @GetMapping(value = "/ask")                                              // option 2 : joins all streamed token chunks into one single String before returning (sync)
    @GetMapping(value = "/ask",                                                 // option 3 : append incoming token chunks to chat UI component dynamically (async)
    produces = MediaType.TEXT_EVENT_STREAM_VALUE
    // produces = MediaType.APPLICATION_NDJSON_VALUE
    )
	public Flux<TokenEvent> askChatbot(@RequestParam String conversationId, @RequestParam String userMessage) 
	{
        // option 1 : returns all token chunks as a single String (sync)
		// return chatbotService.askChatbot(conversationId, userMessage);
        
        // option 2 : joins all streamed token chunks into one single String before returning (sync)
        // return chatbotService.askChatbot(conversationId, userMessage)
        //         .collectList()
        //         .map(list -> String.join("", list))
        //         .block();
        
        // option 3 : append incoming token chunks to chat UI component dynamically (async)
        // DO NOT call .subscribe() here
        // just return the Flux directly so Spring WebFlux can stream it to the client
        return chatbotService.askChatbot(conversationId, userMessage).map(TokenEvent::new);
            // .subscribe(token -> {
            //     // append incoming token chunks to your chat UI component dynamically
            //     ui.access(() -> chatMessageComponent.appendToken(token));
            // });        
	}
}