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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import tech.bigfig.romachat.view.screen.camera.CameraViewModel
import tech.bigfig.romachat.view.screen.cameraresult.CameraResultViewModel
import tech.bigfig.romachat.view.screen.chat.ChatViewModel
import tech.bigfig.romachat.view.screen.chatlist.ChatListViewModel
import tech.bigfig.romachat.view.screen.feed.FeedViewModel
import tech.bigfig.romachat.view.screen.login.LoginViewModel
import tech.bigfig.romachat.view.screen.profile.ProfileViewModel
import tech.bigfig.romachat.view.screen.splash.SplashViewModel
import tech.bigfig.romachat.view.screen.recipient.CameraResultRecipientViewModel
import tech.bigfig.romachat.view.screen.search.UserSearchViewModel
import tech.bigfig.romachat.view.utils.ViewModelFactory

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    abstract fun bindLoginViewModel(viewModel: LoginViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChatListViewModel::class)
    abstract fun bindChatListViewModel(viewModel: ChatListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindChatViewModel(viewModel: ChatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CameraViewModel::class)
    abstract fun bindCameraViewModel(viewModel: CameraViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CameraResultViewModel::class)
    abstract fun bindCameraResultViewModel(viewModel: CameraResultViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CameraResultRecipientViewModel::class)
    abstract fun bindCameraResultRecipientViewModel(viewModel: CameraResultRecipientViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserSearchViewModel::class)
    abstract fun bindUserSearchViewModel(viewModel: UserSearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FeedViewModel::class)
    abstract fun bindFeedViewModel(viewModel: FeedViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(viewModel: ProfileViewModel): ViewModel
}