package com.example.hangmanapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HangmanGameApp()
        }
    }
}

@Composable
fun HangmanGameApp(gameViewModel: GameViewModel = viewModel()) {
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Choose layout based on device orientation
    if (isLandscape) {
        LandscapeLayout(gameViewModel)
    } else {
        PortraitLayout(gameViewModel)
    }
}


// use ViewModel data type to store past reconfiguration
class GameViewModel : ViewModel() {
    private val wordList = listOf("Noise", "Coffee", "Sailor", "Morning", "Star")
    private val hintList = listOf("Any unwanted or disruptive sound", "One of the most popular beverages worldwide", "Person who works aboard boats and ships", "Word comes from the old English word Morgen", "Massive, luminous sphere of plasma")
    private val maxWrongGuesses = 6

    // mutable variables for the GameViewModel class
    var secretWord by mutableStateOf("")
    var displayedWord by mutableStateOf("")
    var wrongGuesses by mutableStateOf(0)
    var gameOver by mutableStateOf(false)
    var letters by mutableStateOf(('A'..'Z').toList())
    var usedLetters by mutableStateOf(setOf<Char>())
    var hintCount by mutableStateOf(0)
    var hintMessage by mutableStateOf("")
    var secretIndex by mutableStateOf(0)

    init {
        startNewGame()
    }

    // Starts a new game
    fun startNewGame() {
        secretIndex = Random.nextInt(wordList.size)
        secretWord = wordList[secretIndex].uppercase()
        displayedWord = "_".repeat(secretWord.length)
        wrongGuesses = 0
        gameOver = false
        letters = ('A'..'Z').toList()
        usedLetters = setOf()
        hintCount = 0
        hintMessage = ""
    }

    // Processes a guess
    fun guessLetter(letter: Char) {
        // Called by buttonOnClick in Letter Panel composable
        if (gameOver) return
        usedLetters = usedLetters + letter

        if (secretWord.contains(letter)) {
            val newDisplayedWord = secretWord.map { if (usedLetters.contains(it)) it else '_' }.joinToString("")
            displayedWord = newDisplayedWord
            if (!displayedWord.contains('_')) {
                gameOver = true
            }
        } else {
            wrongGuesses++
            if (wrongGuesses >= maxWrongGuesses) {
                gameOver = true
            }
        }
    }

    // Handles hint button clicks (takes no arguments and returns nothing)
    fun onHintButtonClick(onHintNotAvailable: () -> Unit ) {

        if (gameOver) return

        if (wrongGuesses >= maxWrongGuesses - 1 && hintCount >= 1) {
            onHintNotAvailable()
            return
        }

        when (hintCount) {
            0 -> {
                hintMessage = hintList[secretIndex]
            }
            1 -> {
                val remainingLetters = letters.filter { !usedLetters.contains(it) && !secretWord.contains(it) }
                val lettersToDisable = remainingLetters.shuffled().take(remainingLetters.size / 2)
                usedLetters = usedLetters + lettersToDisable
                wrongGuesses++
            }
            2 -> {
                val vowels = listOf('A', 'E', 'I', 'O', 'U')
                val vowelsInWord = vowels.filter { secretWord.contains(it) }
                usedLetters = usedLetters + vowels
                vowelsInWord.forEach { guessLetter(it) }
                wrongGuesses++
            }
        }
        hintCount++
    }
}

// Provides the layout for Landscape orientatiom
@Composable
fun LandscapeLayout(gameViewModel: GameViewModel) {
    Row(modifier = Modifier
        .fillMaxSize()
        .background(Color.LightGray)
    ) {
        Column() // Organizes letter and hints panel to the left side of the screen
        {
            LetterPanel(gameViewModel, modifier = Modifier.weight(3f))
            HintPanel(gameViewModel, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(180.dp))
        Column() // Organizes guesses panel to the right side of the screen
        {
            GuessesPanel(gameViewModel, modifier = Modifier.weight(1f))
        }
    }
}

// Provides the layout for Portrait orientatiom
@Composable
fun PortraitLayout(gameViewModel: GameViewModel) {
    // Organizes the guesses and letter panels into a column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hint button is omitted in portrait mode
        GuessesPanel(gameViewModel, modifier = Modifier.weight(2f))
        LetterPanel(gameViewModel, modifier = Modifier
            .weight(1f)
        )

    }
}

// Panel with the letter buttons
@Composable
fun LetterPanel(gameViewModel: GameViewModel, modifier: Modifier = Modifier) {
    val letters = gameViewModel.letters
    val usedLetters = gameViewModel.usedLetters

    Column(modifier = modifier.padding(8.dp),) {
        // Chunked splits letters into a list of lists with max size 7 for each nested list
        val rows = letters.chunked(7)
        rows.forEach { rowLetters ->
            Row {
                rowLetters.forEach { letter ->
                    Button(
                        onClick = { gameViewModel.guessLetter(letter) },
                        // Enabled is a button parameter that controls the state of the button (grays it out)
                        enabled = !usedLetters.contains(letter),
                        modifier = Modifier
                            .padding(2.dp)
                            .height(50.dp)
                            .width(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF143fdb),
                            contentColor = Color.White
                        )

                    ) {
                        Text(
                            letter.toString(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

//Panel with the Hint button and text
@Composable
fun HintPanel(gameViewModel: GameViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(8.dp)) {
        Button(onClick = {
            gameViewModel.onHintButtonClick(onHintNotAvailable = {
                Toast.makeText(context, "Hint not available", Toast.LENGTH_SHORT).show()
            })
        },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF143fdb),
                contentColor = Color.White
            )
        ) {
            Text(text="Hint")
        }

        if (gameViewModel.hintCount > 0 && gameViewModel.hintMessage.isNotEmpty()) {
            Text(gameViewModel.hintMessage)
        }
    }
}

//Panel with the gallow image and correct letters display
@Composable
fun GuessesPanel(gameViewModel: GameViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Abstraction of the img stuff to the HangmanFigure composable
        HangmanFigure(wrongGuesses = gameViewModel.wrongGuesses)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = gameViewModel.displayedWord.map { if (it == '_') '_' else it }.joinToString(" "),
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { gameViewModel.startNewGame() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF143fdb),
                contentColor = Color.White
            )
        ) {
            Text(
                text="New Game",
                fontSize = 15.sp
            )
        }

        if (gameViewModel.gameOver) {
            if (gameViewModel.wrongGuesses >= 6) {
                Text(
                    text="You lost! The word was ${gameViewModel.secretWord}",
                    fontSize = 15.sp,
                    color = Color(0xFFc70404)
                )
            } else {
                Text(
                    text="You won!",
                    fontSize = 15.sp,
                    color = Color(0xFF08a121)
                )
            }
        }
    }
}

// Panel with the Gallow img
@Composable
fun HangmanFigure(wrongGuesses: Int) {
    val hangmanImages = listOf(
        R.drawable.empty_gallow,
        R.drawable.guess_one,
        R.drawable.guess_two,
        R.drawable.guess_three,
        R.drawable.guess_four,
        R.drawable.guess_five,
        R.drawable.guess_six
    )

    // Prevents wrongGuesses from going above the number of images there are
    val imageResource = if (wrongGuesses < hangmanImages.size) {
        hangmanImages[wrongGuesses]
    } else {
        // Show guess six if wrongGuesses is greater than the number of images
        hangmanImages.last()
    }

    // Display the correct image for the number of guesses
    Image(
        painter = painterResource(id = imageResource),
        contentDescription = "Hangman State",
        modifier = Modifier.size(200.dp),
    )

}
