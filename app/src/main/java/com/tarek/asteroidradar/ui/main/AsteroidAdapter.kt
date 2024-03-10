/*
 * MIT License Copyright (c) 2021. Tarek Bohdima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This project was submitted by Tarek Bohdima as part of the Android Kotlin
 * Developer Nanodegree At Udacity. As part of Udacity Honor code, your
 * submissions must be your own work, hence submitting this project as yours will
 * cause you to break the Udacity Honor Code and the suspension of your account.
 * I, the author of the project, allow you to check the code as a reference, but
 * if you submit it, it's your own responsibility if you get expelled.
 */

package com.tarek.asteroidradar.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tarek.asteroidradar.databinding.ItemViewListAsteroidsBinding
import com.tarek.asteroidradar.domain.Asteroid

class AsteroidAdapter(private val clickListener: AsteroidListener) :
    ListAdapter<Asteroid, AsteroidAdapter.AsteroidItemViewHolder>(AsteriodDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AsteroidItemViewHolder {
        return AsteroidItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AsteroidItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, clickListener)
    }

    class AsteroidItemViewHolder private constructor(
        private val viewDataBinding: ItemViewListAsteroidsBinding,
        ) :
        RecyclerView.ViewHolder(viewDataBinding.root) {
        fun bind(asteroid: Asteroid, clickListener: AsteroidListener) {
            viewDataBinding.asteroid = asteroid
            viewDataBinding.clickListener = clickListener
            viewDataBinding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): AsteroidItemViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val itemBinding = ItemViewListAsteroidsBinding.inflate(
                    layoutInflater,
                    parent, false
                )
                return AsteroidItemViewHolder(itemBinding)
            }
        }
    }
}

class AsteriodDiffCallback : DiffUtil.ItemCallback<Asteroid>() {
    override fun areItemsTheSame(oldItem: Asteroid, newItem: Asteroid): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Asteroid, newItem: Asteroid): Boolean {
        return oldItem == newItem
    }

}

class AsteroidListener(val clickListener: (asteroid: Asteroid) -> Unit) {
    fun onClick(asteroid: Asteroid) = clickListener(asteroid)
}
