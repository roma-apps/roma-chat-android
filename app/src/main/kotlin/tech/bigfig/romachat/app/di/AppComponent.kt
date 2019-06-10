/*
 * Copyright (c) 2019 Vasily Kabunov
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>.
 */

package tech.bigfig.romachat.app.di

import dagger.Component
import tech.bigfig.romachat.view.screen.camera.CameraFragment
import tech.bigfig.romachat.view.screen.cameraresult.CameraResultFragment
import tech.bigfig.romachat.view.screen.chat.ChatFragment
import tech.bigfig.romachat.view.screen.chat.ChatMessagesService
import tech.bigfig.romachat.view.screen.chatlist.ChatListFragment
import tech.bigfig.romachat.view.screen.compose.NewPostFragment
import tech.bigfig.romachat.view.screen.feed.FeedFragment
import tech.bigfig.romachat.view.screen.feed.ProfileInsideFeedFragment
import tech.bigfig.romachat.view.screen.login.LoginActivity
import tech.bigfig.romachat.view.screen.profile.ProfileFragment
import tech.bigfig.romachat.view.screen.recipient.CameraResultRecipientFragment
import tech.bigfig.romachat.view.screen.search.UserSearchFragment
import tech.bigfig.romachat.view.screen.splash.SplashActivity
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, DataModule::class, ViewModelModule::class])
interface AppComponent {

    fun inject(fragment: SplashActivity)

    fun inject(activity: LoginActivity)

    fun inject(fragment: ChatListFragment)

    fun inject(fragment: ChatFragment)

    fun inject(fragment: CameraFragment)

    fun inject(fragment: CameraResultFragment)

    fun inject(fragment: CameraResultRecipientFragment)

    fun inject(fragment: UserSearchFragment)

    fun inject(fragment: FeedFragment)

    fun inject(fragment: ProfileFragment)

    fun inject(fragment: ProfileInsideFeedFragment)

    fun inject(fragment: NewPostFragment)

    fun inject(service: ChatMessagesService)
}
