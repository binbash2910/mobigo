import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IBooking, NewBooking } from '../booking.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IBooking for edit and NewBookingFormGroupInput for create.
 */
type BookingFormGroupInput = IBooking | PartialWithRequiredKeyOf<NewBooking>;

type BookingFormDefaults = Pick<NewBooking, 'id'>;

type BookingFormGroupContent = {
  id: FormControl<IBooking['id'] | NewBooking['id']>;
  nbPlacesReservees: FormControl<IBooking['nbPlacesReservees']>;
  montantTotal: FormControl<IBooking['montantTotal']>;
  dateReservation: FormControl<IBooking['dateReservation']>;
  statut: FormControl<IBooking['statut']>;
  trajet: FormControl<IBooking['trajet']>;
  passager: FormControl<IBooking['passager']>;
};

export type BookingFormGroup = FormGroup<BookingFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class BookingFormService {
  createBookingFormGroup(booking: BookingFormGroupInput = { id: null }): BookingFormGroup {
    const bookingRawValue = {
      ...this.getFormDefaults(),
      ...booking,
    };
    return new FormGroup<BookingFormGroupContent>({
      id: new FormControl(
        { value: bookingRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      nbPlacesReservees: new FormControl(bookingRawValue.nbPlacesReservees, {
        validators: [Validators.required],
      }),
      montantTotal: new FormControl(bookingRawValue.montantTotal, {
        validators: [Validators.required],
      }),
      dateReservation: new FormControl(bookingRawValue.dateReservation, {
        validators: [Validators.required],
      }),
      statut: new FormControl(bookingRawValue.statut, {
        validators: [Validators.required],
      }),
      trajet: new FormControl(bookingRawValue.trajet),
      passager: new FormControl(bookingRawValue.passager),
    });
  }

  getBooking(form: BookingFormGroup): IBooking | NewBooking {
    return form.getRawValue() as IBooking | NewBooking;
  }

  resetForm(form: BookingFormGroup, booking: BookingFormGroupInput): void {
    const bookingRawValue = { ...this.getFormDefaults(), ...booking };
    form.reset(
      {
        ...bookingRawValue,
        id: { value: bookingRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): BookingFormDefaults {
    return {
      id: null,
    };
  }
}
