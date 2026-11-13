package com.novo.drive.presentation.di

import com.novo.drive.presentation.AppViewModel
import com.novo.drive.presentation.auth.KunjoAuthViewModel
import com.novo.drive.presentation.channels.ChannelsViewModel
import com.novo.drive.presentation.cleaner.StorageCleanerViewModel
import com.novo.drive.presentation.collection.CollectionViewModel
import com.novo.drive.presentation.family.FamilyDashboardViewModel
import com.novo.drive.presentation.files.IplusFileClipboard
import com.novo.drive.presentation.files.IplusFilesViewModel
import com.novo.drive.presentation.gallery.GalleryViewModel
import com.novo.drive.presentation.home.HomeViewModel
import com.novo.drive.presentation.iplusmessage.IplusMessageViewModel
import com.novo.drive.presentation.music.IplusMusicPlayerViewModel
import com.novo.drive.presentation.onboarding.OnboardingViewModel
import com.novo.drive.presentation.note.NoteEditorViewModel
import com.novo.drive.presentation.preview.PreviewContentResolver
import com.novo.drive.presentation.preview.PreviewViewModel
import com.novo.drive.presentation.proxy.ProxyViewModel
import com.novo.drive.presentation.search.SearchViewModel
import com.novo.drive.presentation.settings.ExclusionsViewModel
import com.novo.drive.presentation.settings.SettingsViewModel
import com.novo.drive.presentation.stream.StreamBridgeViewModel
import com.novo.drive.presentation.transfers.TransfersViewModel
import com.novo.drive.presentation.trash.TrashViewModel
import com.novo.drive.presentation.video.IplusVideoPlayerViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedUiModule = module {
    singleOf(::PreviewContentResolver)
    singleOf(::IplusFileClipboard)
    viewModelOf(::AppViewModel)
    viewModelOf(::PreviewViewModel)
    viewModelOf(::ChannelsViewModel)
    viewModelOf(::CollectionViewModel)
    viewModelOf(::IplusFilesViewModel)
    viewModelOf(::GalleryViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::NoteEditorViewModel)
    viewModelOf(::ProxyViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ExclusionsViewModel)
    viewModelOf(::TransfersViewModel)
    viewModelOf(::TrashViewModel)
    viewModelOf(::FamilyDashboardViewModel)
    viewModelOf(::StorageCleanerViewModel)
    viewModelOf(::IplusMusicPlayerViewModel)
    viewModelOf(::IplusVideoPlayerViewModel)
    viewModelOf(::StreamBridgeViewModel)
    viewModelOf(::KunjoAuthViewModel)
    viewModelOf(::IplusMessageViewModel)
}
