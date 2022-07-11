package com.example.rickandmorty

import android.app.SearchManager
import android.widget.SearchView.OnQueryTextListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.widget.SearchView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedInputStream
import java.io.InputStream
import java.math.RoundingMode
import java.net.URL
import java.net.URLConnection
import java.text.DecimalFormat

class Character(val id: String, val name: String, val species: String, val gender: String, val status: String, val img : Bitmap)


class MainActivity : AppCompatActivity() {
    private lateinit var tx: TextView
    private lateinit var prog: TextView
    private lateinit var im: ImageView

    var characters : MutableList<Character> = ArrayList() // все персонажи
    var names : MutableList<String> = ArrayList() // имена персонажей, нужно для поиска
    var data = IntArray(3,{-1})


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tx = findViewById(R.id.textView)
        prog = findViewById(R.id.textView2)
        im = findViewById(R.id.imageView2)

        Thread({
            try {
                var url = "https://rickandmortyapi.com/"
                var document: Document = Jsoup.connect(url).ignoreContentType(true).get()
                var soup = document.getElementsByClass("caption__Caption-sc-w9tm9f-0 lpmWNO") // пeрсонажи, локации, эпизоды
                for (i in 0 until 3) {
                    var fi: String = soup[i].text()
                    var result = fi.replace(Regex("[^0-9]"), "")
                    data[i] = result.toInt()
                }
                runOnUiThread({
                    tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, (38).toFloat())
                    tx.setLineSpacing((1.5).toFloat(), (1.5).toFloat())
                    tx.setText("Characters: ${data[0]}\nLocations: ${data[1]}\nEpisodes: ${data[2]}")
                    tx.setVisibility(TextView.VISIBLE)
                })
                // теперь нужно собрать информацию о всех персонажах
                url = "https://rickandmortyapi.com/api/character/"
                var current_page :Int = 1
                var total_pages : Int = data[0]
                while (current_page <= total_pages) {
                    document = Jsoup.connect(url + current_page.toString()).ignoreContentType(true).get()
                    val text : String = document.text()
                    var current : Character = Character(my_get("id", text, 1), my_get("name", text, 0), my_get("species", text, 0), my_get("gender", text, 0), my_get("status", text, 0), getImageBitmap(my_get("image", text, 0)))
                    characters.add(current)
                    names.add(my_get("name", text, 0).lowercase())
                    current_page += 1
                    runOnUiThread({
                        val df = DecimalFormat("#.##")
                        df.roundingMode = RoundingMode.DOWN
                        val roundoff = df.format(100 * (current_page.toFloat()) / total_pages)
                        prog.setText("Loading ${roundoff}%")
                        prog.setVisibility(TextView.VISIBLE)
                    })
                }
                runOnUiThread({
                    prog.setVisibility(TextView.INVISIBLE)
                })
            }
            catch (e: Exception) {
                runOnUiThread({
                    tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, (32).toFloat())
                    tx.setText("An error occurred!\nPlease, check your internet connection.")
                    tx.setVisibility(TextView.VISIBLE)
                })
            }
        }).start()
    }

    fun my_get(key: String, text : String, flag : Int) : String {
        var ans : String = ""
        var index = text.indexOf(key)
        if (flag == 0)
            index += key.length + 3
        else
            index += key.length + 2
        while (text[index] != '"' && text[index] != ',') {
            ans += text[index]
            ++index
        }
        return ans
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        val manager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchItem = menu?.findItem(R.id.search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                searchView.setQuery("", false)
                searchItem.collapseActionView()
                Toast.makeText(this@MainActivity, "Looking for $query", Toast.LENGTH_LONG).show()
                val ind = query?.let { names.indexOf(it.lowercase()) }
                if (ind == -1) {
                    tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, (32).toFloat())
                    tx.setText("Not found")
                    tx.setVisibility(TextView.VISIBLE)
                    im.setVisibility(ImageView.INVISIBLE)
                }
                else {
                    tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, (32).toFloat())
                    if (ind != null) {
                        tx.setText("Name: ${characters[ind].name}\nSpecies: ${characters[ind].species}\nGender: ${characters[ind].gender}\n" +
                                "Status: ${characters[ind].status}\nId: ${characters[ind].id}")
                        im.setImageBitmap(characters[ind].img)
                        im.setVisibility(ImageView.VISIBLE)
                    }
                    tx.setVisibility(TextView.VISIBLE)
                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }

        })
        return true
    }

    private fun getImageBitmap(url: String): Bitmap {
        var bm : Bitmap? = null
        var aURL : URL = URL(url)
        var conn : URLConnection = aURL.openConnection()
        conn.connect()
        var fi : InputStream = conn.getInputStream()
        var bis : BufferedInputStream = BufferedInputStream(fi)
        bm = BitmapFactory.decodeStream(bis)
        bis.close()
        fi.close()
        return bm
    }

}