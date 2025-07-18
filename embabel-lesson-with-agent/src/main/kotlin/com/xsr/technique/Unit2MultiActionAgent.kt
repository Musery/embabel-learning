package com.xsr.technique

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.prompt.persona.Persona
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria
import com.embabel.common.core.types.Timestamped
import org.springframework.beans.factory.annotation.Value
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val StoryTeller = Persona(
    name = "Roald Dahl",
    persona = "A creative storyteller who loves to weave imaginative tales that are a bit unconventional",
    voice = "Quirky",
    objective = "Create memorable stories that captivate the reader's imagination.",
)

val Reviewer = Persona(
    name = "Media Book Review",
    persona = "New York Times Book Reviewer",
    voice = "Professional and insightful",
    objective = "Help guide readers toward good stories",
)

data class Story(
    val text: String,
)

data class ReviewedStory(
    val story: Story,
    val review: String,
    val reviewer: Persona,
) : HasContent, Timestamped {

    override val timestamp: Instant
        get() = Instant.now()

    override val content: String
        get() = """
            # Story
            ${story.text}

            # Review
            $review

            # Reviewer
            ${reviewer.name}, ${
            timestamp.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
        }
        """.trimIndent()
}


@Agent(
    description = "Generate a story based on user input and review it",
)
class WriteAndReviewAgent(
    @Value("\${storyWordCount:100}") private val storyWordCount: Int,
    @Value("\${reviewWordCount:100}") private val reviewWordCount: Int,
) {

    @Action
    fun craftStory(userInput: UserInput): Story =
        using(
            LlmOptions(criteria = ModelSelectionCriteria.Auto)
                .withTemperature(.9), // Higher temperature for more creative output
        ).withPromptContributor(StoryTeller)
            .create(
                """
            Craft a short story in $storyWordCount words or less.
            The story should be engaging and imaginative.
            Use the user's input as inspiration if possible.
            If the user has provided a name, include it in the story.

            # User input
            ${userInput.content}
        """.trimIndent()
            )

    @AchievesGoal("The user has been greeted")
    @Action
    fun reviewStory(userInput: UserInput, story: Story, context: OperationContext): ReviewedStory {
        val review = context.promptRunner(
            LlmOptions(criteria = ModelSelectionCriteria.Auto)
        ).withPromptContributor(Reviewer)
            .generateText(
                """
            You will be given a short story to review.
            Review it in $reviewWordCount words or less.
            Consider whether or not the story is engaging, imaginative, and well-written.
            Also consider whether the story is appropriate given the original user input.

            # Story
            ${story.text}

            # User input that inspired the story
            ${userInput.content}
        """.trimIndent()
            )
        return ReviewedStory(
            story = story,
            review = review,
            reviewer = Reviewer,
        )
    }

}