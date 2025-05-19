import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { IVehicle } from '../vehicle.model';

@Component({
  selector: 'jhi-vehicle-detail',
  templateUrl: './vehicle-detail.component.html',
  imports: [SharedModule, RouterModule],
})
export class VehicleDetailComponent {
  vehicle = input<IVehicle | null>(null);

  previousState(): void {
    window.history.back();
  }
}
