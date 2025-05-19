import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IRide } from 'app/entities/ride/ride.model';
import { RideService } from 'app/entities/ride/service/ride.service';
import { IStep } from '../step.model';
import { StepService } from '../service/step.service';
import { StepFormGroup, StepFormService } from './step-form.service';

@Component({
  selector: 'jhi-step-update',
  templateUrl: './step-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class StepUpdateComponent implements OnInit {
  isSaving = false;
  step: IStep | null = null;

  ridesSharedCollection: IRide[] = [];

  protected stepService = inject(StepService);
  protected stepFormService = inject(StepFormService);
  protected rideService = inject(RideService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: StepFormGroup = this.stepFormService.createStepFormGroup();

  compareRide = (o1: IRide | null, o2: IRide | null): boolean => this.rideService.compareRide(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ step }) => {
      this.step = step;
      if (step) {
        this.updateForm(step);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const step = this.stepFormService.getStep(this.editForm);
    if (step.id !== null) {
      this.subscribeToSaveResponse(this.stepService.update(step));
    } else {
      this.subscribeToSaveResponse(this.stepService.create(step));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStep>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(step: IStep): void {
    this.step = step;
    this.stepFormService.resetForm(this.editForm, step);

    this.ridesSharedCollection = this.rideService.addRideToCollectionIfMissing<IRide>(this.ridesSharedCollection, step.trajet);
  }

  protected loadRelationshipsOptions(): void {
    this.rideService
      .query()
      .pipe(map((res: HttpResponse<IRide[]>) => res.body ?? []))
      .pipe(map((rides: IRide[]) => this.rideService.addRideToCollectionIfMissing<IRide>(rides, this.step?.trajet)))
      .subscribe((rides: IRide[]) => (this.ridesSharedCollection = rides));
  }
}
