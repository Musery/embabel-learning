package com.xsr.technique;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.RequireNameMatch;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.prompt.persona.Persona;

//@Agent(description = "主题故事生成")
public class JavaUnit4ParallelAgent {

    @Action(outputBinding = "topic")
    public String getTopicFromInput(UserInput userInput, OperationContext context) {
        return context.ai().withAutoLlm()
                .createObject(
                        String.format("根据用户输入的内容\"%s\"提取想要生成的主题信息, 字数不超过10个", userInput.getContent()),
                        String.class
                );
    }

    @Action(outputBinding = "chinese")
    public Story chineseStory(@RequireNameMatch String topic, OperationContext context) {
        return createStory(topic, context, Unit4ParallelAgentKt.getChinese());
    }

    @Action(outputBinding = "european")
    public Story europeanStory(@RequireNameMatch String topic, OperationContext context) {
        return createStory(topic, context, Unit4ParallelAgentKt.getEuropean());
    }

    @AchievesGoal(description = "根据主题创建多个文化体系的小说")
    @Action(outputBinding = "merge")
    public String merge(@RequireNameMatch Story chinese, @RequireNameMatch Story european, OperationContext context) {
        return "中国故事:" + chinese.getContent() + "\n" +
                "欧洲故事" + european.getContent();
    }

    public Story createStory(String topic, OperationContext context, Persona persona) {
        return context.ai().withAutoLlm().withPromptContributor(persona).createObject(
                String.format("""
                        根据主题:%s创造不超过50字的小故事
                        """, topic), Story.class
        );
    }

}

