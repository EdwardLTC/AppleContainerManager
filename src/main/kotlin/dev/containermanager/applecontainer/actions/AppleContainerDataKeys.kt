package dev.containermanager.applecontainer.actions

import com.intellij.openapi.actionSystem.DataKey
import dev.containermanager.applecontainer.cli.model.ContainerInfo
import dev.containermanager.applecontainer.cli.model.ImageInfo
import dev.containermanager.applecontainer.cli.model.NetworkInfo
import dev.containermanager.applecontainer.cli.model.VolumeInfo

object AppleContainerDataKeys {
    val SELECTED_CONTAINERS: DataKey<List<ContainerInfo>> = DataKey.create("AppleContainerManager.SelectedContainers")
    val SELECTED_IMAGES: DataKey<List<ImageInfo>> = DataKey.create("AppleContainerManager.SelectedImages")
    val SELECTED_VOLUMES: DataKey<List<VolumeInfo>> = DataKey.create("AppleContainerManager.SelectedVolumes")
    val SELECTED_NETWORKS: DataKey<List<NetworkInfo>> = DataKey.create("AppleContainerManager.SelectedNetworks")
}
